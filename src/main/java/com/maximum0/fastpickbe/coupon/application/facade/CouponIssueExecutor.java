package com.maximum0.fastpickbe.coupon.application.facade;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponIssueExecutor {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    /**
     * 실제 쿠폰 발급 트랜잭션을 수행합니다.
     *
     * @param couponId 발급할 쿠폰 식별자
     * @param user     발급 대상 사용자
     * @param now      발급 시각
     * @return Long    생성된 발급 이력 ID
     */
    @Transactional
    public Long executeIssue(Long couponId, User user, LocalDateTime now) {
        Coupon coupon = couponRepository.findActiveById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        if (issuedCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED_COUPON);
        }

        coupon.issue(now);

        IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon);
        return issuedCouponRepository.save(issuedCoupon).getId();
    }
}
