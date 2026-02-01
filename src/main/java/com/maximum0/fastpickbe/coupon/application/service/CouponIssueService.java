package com.maximum0.fastpickbe.coupon.application.service;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.application.facade.CouponIssueFacade;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 관련 비즈니스 로직을 담당하는 서비스.
 *
 * - 쿠폰 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final RedissonClient redissonClient;
    private final CouponIssueFacade couponIssueFacade;
    private final Clock clock;

    private static final String LOCK_PREFIX = "lock:coupon:";

    /**
     * 분산 락을 획득하여 동시 발급 상황에서도 쿠폰 발급을 안전하게 처리한다.
     *
     * - 쿠폰 단위로 락을 획득한다.
     * - 락 획득 실패 시 즉시 예외를 반환한다.
     * - 실제 발급 로직은 파사드 레이어에 위임한다.
     *
     * @throws BusinessException 락 획득 실패(CONCURRENCY_BUSY) 또는 시스템 인터럽트 오류 시
     */
    public Long issue(Long couponId, User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        String lockKey = LOCK_PREFIX + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean available = lock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!available) {
                log.warn("⚠️ [CouponIssueService] 락 획득 실패 - 트래픽 과부하 | couponId={}, userId={}", couponId, user.getId());
                throw new BusinessException(ErrorCode.CONCURRENCY_BUSY);
            }

            Long issuedId = couponIssueFacade.executeIssue(couponId, user, now);
            log.info("✅ [CouponIssueService] 발급 완료 | couponId={}, userId={}, issuedId={}", couponId, user.getId(), issuedId);
            return issuedId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("⛔️ [CouponIssueService] 락 획득 중단 - 시스템 인터럽트 발생 | couponId={}, userId={}", couponId, user.getId());
            throw new BusinessException(ErrorCode.SYSTEM_LOCKING_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
