package com.maximum0.fastpickbe.coupon.application.processor;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueBatchWorker {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final CouponIssueProcessor couponIssueProcessor;
    private final Clock clock;

    /**
     * 큐에서 추출된 유저 ID 목록을 DB와 매핑하고 실제 발급 프로세서(CouponIssueProcessor)를 실행한다.
     *
     * - 큐에 이미 존재하는 데이터를 DB에서 조회할 수 없으면 에러로 간주한다. (데이터 정합성이 깨진 상태)
     * - 개별 발급 실패가 전체 Batch 작업에 영향을 주지 않도록 내부 try-catch 를 유지한다.
     * - 유저 정보 Bulk 조회를 통해 N+1 Select 문제를 방지한다.
     * - 개별 발급 성공 건수를 집계하여 쿠폰 수량을 단일 벌크 쿼리로 업데이트(N+1 Write 방지)한다.
     *
     * @throws BusinessException 쿠폰 미존재(COUPON_NOT_FOUND)
     */
    @Transactional
    public void issueBatch(Long couponId, Collection<ScoredEntry<String>> batch) {
        Coupon coupon = couponRepository.findActiveById(couponId)
                .orElseThrow(() -> {
                    log.error("⛔ [Coupon-Worker] 유효하지 않은 쿠폰 ID | couponId={}", couponId);
                    return new BusinessException(ErrorCode.COUPON_NOT_FOUND);
                });

        Map<Long, User> userMap = fetchUserMap(batch);

        long issuedCount = batch.stream()
                .filter(entry -> issueCouponToUser(coupon, entry, userMap.get(Long.valueOf(entry.getValue()))))
                .count();

        if (issuedCount > 0) {
            couponRepository.incrementIssuedQuantity(couponId, (int) issuedCount);
        }

        log.info("✅ [Coupon-Worker] 배치 처리 완료 | couponId={}, count={}", couponId, batch.size());
    }

    /**
     * 배치에 포함된 유저 ID 목록을 기반으로 조회하여 Map 형태로 변환한다.
     *
     * @param batch Redis에서 인출한 유저 ID 및 스코어 묶음
     * @return 유저 ID를 Key로 하는 User 엔티티 맵
     */
    private Map<Long, User> fetchUserMap(Collection<ScoredEntry<String>> batch) {
        List<Long> userIds = batch.stream()
                .map(entry -> Long.valueOf(entry.getValue()))
                .toList();

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (existing, replacement) -> existing));
    }

    /**
     * 개별 유저에게 쿠폰을 발급하고 성공 여부를 반환한다.
     *
     * @param coupon 발급 대상 쿠폰 엔티티
     * @param entry  Redis 대기열 항목 (ID, Score)
     * @param user   조회된 유저 엔티티 (nullable)
     * @return 발급 성공 시 true, 유저 미존재 또는 로직 실패 시 false
     */
    private boolean issueCouponToUser(Coupon coupon, ScoredEntry<String> entry, User user) {
        Long userId = Long.valueOf(entry.getValue());
        if (user == null) {
            log.error("⛔ [Coupon-Worker] 존재하지 않는 유저 스킵 | userId={}", userId);
            return false;
        }

        try {
            LocalDateTime requestTime = parseRequestTime(entry.getScore());
            couponIssueProcessor.execute(coupon, user, requestTime);
            return true;
        } catch (Exception e) {
            log.warn("⚠️ [Coupon-Worker] 개별 발급 처리 실패 | userId={}, couponId={}, error={}", userId, coupon.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Redis의 Score 값을 시스템의 LocalDateTime으로 변환한다.
     *
     * @param score Redis Sorted Set에 저장된 밀리초 단위 타임스탬프
     * @return 변환된 요청 시간
     */
    private LocalDateTime parseRequestTime(Double score) {
        long millis = Math.round(score);
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis),
                clock.getZone()
        );
    }
}
