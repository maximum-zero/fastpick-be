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

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final RedissonClient redissonClient;
    private final CouponIssueFacade couponIssueFacade;
    private final Clock clock;

    private static final String LOCK_PREFIX = "lock:coupon:";

    /**
     * 분산 락을 획득하여 안전하게 쿠폰 발급을 처리한다.
     * 락 획득을 위해 최대 10초간 대기하며, 획득 성공 시 파사드 레이어에 발급 로직을 위임한다.
     *
     * @param couponId 발급할 쿠폰 식별자
     * @param user     발급 대상 사용자 엔티티
     * @return 생성된 쿠폰 발급 이력 식별자(ID)
     * @throws BusinessException 락 획득 실패(CONCURRENCY_BUSY) 또는 시스템 인터럽트 오류 시
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
            return couponIssueFacade.executeIssue(couponId, user, now);
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
