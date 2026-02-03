package com.maximum0.fastpickbe.coupon.application.service;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.infra.redis.CouponRedisSchema;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 관련 비즈니스 로직을 담당하는 서비스.
 *
 * - Redis Atomic Operation을 사용하여 락 없이 고성능 재고 차감 처리한다.
 * - 발급 성공 시 대기열(Redis Scored Sorted Set)에 적재한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {
    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final Clock clock;

    /**
     * Redis 기반의 선착순 검증 및 발급 대기열 적재를 수행한다.
     *
     * 1. Redis 내 재고 데이터 존재 유무 확인 및 Warm-up
     * 2. Redis 내 종료 시간으로 발급 기간 검증
     * 3. 유저별 중복 발급 여부 체크 및 실시간 재고 차감
     * 4. 발급 대상자 기준 발급 대기열 큐에 적재 및 활성 큐 목록에 등록
     *
     * @throws BusinessException 발급 조건 미충족(기간, 재고, 중복 등) 또는 시스템 대기열 진입 실패 시
     */
    public void issue(Long couponId, Long userId) {
        String stockKey = CouponRedisSchema.getStockKey(couponId);
        String userSetKey = CouponRedisSchema.getUserSetKey(couponId);

        RAtomicLong stock = redissonClient.getAtomicLong(stockKey);
        RSet<String> issuedUsers = redissonClient.getSet(userSetKey);

        // Redis 내 재고 데이터 존재 유무 확인 및 Warm-up
        if (!stock.isExists()) {
            ensureStockInitialized(couponId, stock, issuedUsers);
        }

        // 쿠폰 발급 기간 확인
        checkIssuablePeriodWithCache(couponId);

        // 유저별 중복 발급 여부 체크 및 실시간 재고 차감
        validateAndDecrementStock(userId, stock, issuedUsers);

        // 발급 대상자 기준 발급 대기열 큐에 적재 및 활성 큐 목록에 등록
        enqueueRequest(couponId, userId, stock, issuedUsers);
    }

    /**
     * Redis 재고가 없는 경우 DB 조회를 통해 재고량 및 만료일을 확인한다.
     *
     * - 동시에 DB 접근을 방지하기 위해 동기화를 사용한다.
     *
     * @throws BusinessException 쿠폰 미존재(COUPON_NOT_FOUND), 발급 조건 부적합(COUPON_DISABLED, COUPON_NOT_AVAILABLE_PERIOD, COUPON_EXHAUSTED)
     */
    private synchronized void ensureStockInitialized(Long couponId, RAtomicLong stock, RSet<String> issuedUsers) {
        if (stock.isExists()) return;

        try {
            Coupon coupon = couponRepository.findActiveById(couponId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            LocalDateTime now = LocalDateTime.now(clock);
            coupon.validateIssuable(now);

            // Redis TTL 설정
            Duration ttl = coupon.getRemainingIssuanceDuration(now).plusHours(24);

            // 쿠폰 남은 수량 & 발급 종료 시간 설정
            RBucket<Long> expireAt = redissonClient.getBucket(CouponRedisSchema.getExpireAt(couponId));
            long endAtMillis = coupon.getEndAt().atZone(clock.getZone()).toInstant().toEpochMilli();

            stock.set(coupon.getStockQuantity());
            expireAt.set(endAtMillis);

            stock.expire(ttl);
            expireAt.expire(ttl);
            issuedUsers.expire(ttl);

            log.info("✅ [ensureStockInitialized] 쿠폰 재고 및 종료 시간 초기화 완료 | couponId={}, stock={}, expireAt={}", couponId, stock.get(), coupon.getEndAt());
        } catch (Exception e) {
            log.error("⛔ [ensureStockInitialized] 쿠폰 재고 및 종료 시간 초기화 중 오류 발생 | couponId={}, error={}", couponId, e.getMessage());
            throw e;
        }
    }

    /**
     * Redis에 캐싱된 종료 시각을 기준으로 발급 가능 여부를 판정한다.
     *
     * @throws BusinessException 발급 기간 아님(COUPON_NOT_AVAILABLE_PERIOD)
     */
    private void checkIssuablePeriodWithCache(Long couponId) {
        RBucket<String> endAtBucket = redissonClient.getBucket(CouponRedisSchema.getExpireAt(couponId));
        String endAtStr = endAtBucket.get();

        if (endAtStr != null) {
            long endAtMillis = Long.parseLong(endAtStr);

            if (clock.millis() > endAtMillis) {
                log.info("ℹ️ [checkIssuablePeriodWithCache] 발급 종료된 쿠폰 요청 차단 | couponId={}", couponId);
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE_PERIOD);
            }
        }
    }

    /**
     * 중복 발급 여부를 확인하고 Redis 재고를 선차감한다.
     *
     * - 차감 후 재고가 부족한 경우 즉시 수량을 복구한다.
     *
     * @throws BusinessException 중복 발급(ALREADY_ISSUED_COUPON), 재고 소진(COUPON_EXHAUSTED)
     */
    private void validateAndDecrementStock(Long userId, RAtomicLong stock, RSet<String> issuedUsers) {
        if (!issuedUsers.add(userId.toString())) {
            log.info("ℹ️ [validateAndDecrementStock] 쿠폰 중복 발급 시도 | userId={}", userId);
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        if (stock.decrementAndGet() < 0) {
            stock.incrementAndGet();
            issuedUsers.remove(userId.toString());

            log.warn("⚠️ [validateAndDecrementStock] 쿠폰 재고 소진 | userId={}", userId);
            throw new BusinessException(ErrorCode.COUPON_EXHAUSTED);
        }
    }

    /**
     * 발급 대상자를 발급 대기열에 추가한다.
     *
     * - 대기열 등록 실패 시, 재고 차감 및 중복 체크 기록을 모두 원상 복구한다.
     *
     * @throws BusinessException 대기열 진입 실패(SYSTEM_LOCKING_ERROR)
     */
    private void enqueueRequest(Long couponId, Long userId, RAtomicLong stock, RSet<String> issuedUsers) {
        try {
            long requestTime = clock.millis();
            redissonClient.getScoredSortedSet(CouponRedisSchema.getWaitingKey(couponId))
                    .add((double) requestTime, userId.toString());

            // 발급 처리 대상 유저 목록 업데이트
            issuedUsers.add(userId.toString());

            // 활성 큐 식별자 등록
            redissonClient.getSet(CouponRedisSchema.getActiveQueuesKey())
                    .add(couponId.toString());

            log.info("✅ [enqueueRequest] 대기열 진입 완료 | userId={}, couponId={}", userId, couponId);
        } catch (Exception e) {
            stock.incrementAndGet();
            issuedUsers.remove(userId.toString());

            log.error("⛔ [enqueueRequest] 대기열 적재 실패 및 롤백 수행 | userId={}, couponId={}, error={}", userId, couponId, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_LOCKING_ERROR);
        }
    }

}
