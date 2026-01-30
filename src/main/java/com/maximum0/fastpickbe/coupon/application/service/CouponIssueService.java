package com.maximum0.fastpickbe.coupon.application.service;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.application.facade.CouponIssueExecutor;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {
    private final RedissonClient redissonClient;
    private final CouponIssueExecutor couponIssueExecutor;
    private final Clock clock;

    private static final String LOCK_PREFIX = "lock:coupon:";

    /**
     * 쿠폰 발급을 처리합니다.
     *
     * @param couponId 발급할 쿠폰 식별자
     * @param user     발급 대상 사용자
     * @return Long    생성된 발급 이력 ID
     */
    public Long issue(Long couponId, User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        String lockKey = LOCK_PREFIX + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean available = lock.tryLock(10, -1, TimeUnit.SECONDS);
            if (!available) {
                throw new BusinessException(ErrorCode.CONCURRENCY_BUSY);
            }

            log.info("[CouponIssueService] 쿠폰 발급 성공 - 쿠폰ID: {}, 유저ID: {}", couponId, user.getId());
            return couponIssueExecutor.executeIssue(couponId, user, now);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_LOCKING_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}