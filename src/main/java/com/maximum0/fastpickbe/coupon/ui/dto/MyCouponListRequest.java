package com.maximum0.fastpickbe.coupon.ui.dto;

import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponFilterType;

public record MyCouponListRequest(
        String search,
        IssuedCouponFilterType status
) {
    public MyCouponListRequest {
        if (status == null) {
            status = IssuedCouponFilterType.ALL;
        }
    }
}
