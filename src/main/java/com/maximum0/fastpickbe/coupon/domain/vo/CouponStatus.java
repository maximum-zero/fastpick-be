package com.maximum0.fastpickbe.coupon.domain.vo;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰의 비즈니스 생명주기 상태를 정의하고 결정한다.
 */
@Getter
@RequiredArgsConstructor
public enum CouponStatus {

    READY("발급 대기"),
    ISSUING("발급 중"),
    EXHAUSTED("소진됨"),
    EXPIRED("기간 만료"),
    DISABLED("발급 중단")
    ;

    private final String description;

    /**
     * 쿠폰 엔티티와 기준 시각을 바탕으로 현재 쿠폰의 비즈니스 상태를 결정한다.
     * @param coupon 상태를 판단할 쿠폰 엔티티
     * @param now 기준 시각
     * @return 결정된 쿠폰 상태
     */
    public static CouponStatus of(Coupon coupon, LocalDateTime now) {
        if (coupon.getUseStatus() == CouponUseStatus.DISABLED) {
            return DISABLED;
        }

        if (coupon.isReady(now)) {
            return READY;
        }

        if (coupon.isExpired(now)) {
            return EXPIRED;
        }

        if (coupon.isExhausted()) {
            return EXHAUSTED;
        }

        return ISSUING;
    }

}
