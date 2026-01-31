package com.maximum0.fastpickbe.coupon.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 발급된 쿠폰 목록 조회를 위한 필터 타입을 정의한다.
 */
@Getter
@RequiredArgsConstructor
public enum IssuedCouponFilterType {

    ALL("전체"),
    AVAILABLE("사용 가능"),
    USED("사용 완료"),
    EXPIRED("기간 만료")
    ;

    private final String description;

}
