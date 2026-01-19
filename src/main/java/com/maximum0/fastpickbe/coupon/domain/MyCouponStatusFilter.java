package com.maximum0.fastpickbe.coupon.domain;

/**
 * 내 쿠폰 목록 조회 시 사용될 필터링 상태 값입니다.
 */
public enum MyCouponStatusFilter {
    ALL, // 전체
    AVAILABLE, // 사용 가능
    USED, // 사용됨
    EXPIRED // 만료
}
