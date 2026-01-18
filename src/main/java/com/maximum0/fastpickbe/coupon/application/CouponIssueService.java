package com.maximum0.fastpickbe.coupon.application;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    /**
     * 쿠폰 발급을 처리합니다.
     * 비관적 락을 획득하여 동시성 상황에서 정확한 수량 차감을 보장합니다.
     *
     * @param couponId 발급할 쿠폰 식별자
     * @param user     발급 대상 사용자
     * @return Long    생성된 발급 이력 ID
     */
    @Transactional
    public Long issue(Long couponId, User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        if (issuedCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        coupon.issue(now);

        IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon);
        return issuedCouponRepository.save(issuedCoupon).getId();
    }
}