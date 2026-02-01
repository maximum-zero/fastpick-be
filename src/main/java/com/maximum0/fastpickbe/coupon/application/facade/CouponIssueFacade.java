package com.maximum0.fastpickbe.coupon.application.facade;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponIssueFacade {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    /**
     * 쿠폰 발급 프로세스를 원자적으로 수행한다.
     * 쿠폰 존재 여부 확인, 중복 발급 체크, 재고 차감, 발급 이력 저장을 순차적으로 진행한다.
     *
     * @throws BusinessException 쿠폰이 없거나(COUPON_NOT_FOUND), 이미 발급받은 경우(ALREADY_ISSUED_COUPON)
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
