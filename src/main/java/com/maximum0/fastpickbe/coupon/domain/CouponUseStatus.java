package com.maximum0.fastpickbe.coupon.domain;

public enum CouponUseStatus {
    AVAILABLE("사용 가능"),
    DISABLED("사용 중지")
    ;

    private final String description;

    CouponUseStatus(String description) {
        this.description = description;
    }

}
