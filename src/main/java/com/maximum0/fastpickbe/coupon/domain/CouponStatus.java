package com.maximum0.fastpickbe.coupon.domain;

public enum CouponStatus {
    READY("발급 대기"),
    ISSUING("발급 중"),
    EXHAUSTED("소진됨"),
    EXPIRED("기간 만료"),
    DISABLED("발급 중단")
    ;

    private final String description;

    CouponStatus(String description) {
        this.description = description;
    }

}
