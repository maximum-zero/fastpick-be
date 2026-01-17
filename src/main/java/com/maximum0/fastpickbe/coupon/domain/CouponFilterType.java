package com.maximum0.fastpickbe.coupon.domain;

public enum CouponFilterType {
    ALL("전체"),
    READY("발급 예정"),
    ISSUING("발급 중"),
    CLOSED("발급 종료")
    ;

    private final String description;

    CouponFilterType(String description) {
        this.description = description;
    }
}