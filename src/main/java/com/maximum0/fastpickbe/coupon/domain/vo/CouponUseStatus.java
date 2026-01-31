package com.maximum0.fastpickbe.coupon.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰의 활성화 및 사용 가능 여부를 정의한다.
 */
@Getter
@RequiredArgsConstructor
public enum CouponUseStatus {

    AVAILABLE("사용 가능"),
    DISABLED("사용 중지")
    ;

    private final String description;

}
