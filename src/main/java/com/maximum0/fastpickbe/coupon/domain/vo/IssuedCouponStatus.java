package com.maximum0.fastpickbe.coupon.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 발급된 쿠폰의 현재 상태를 정의한다.
 */
@Getter
@RequiredArgsConstructor
public enum IssuedCouponStatus {

    AVAILABLE("사용 가능"),
    USED("사용 완료"),
    EXPIRED("기간 만료")
    ;

    private final String description;

}
