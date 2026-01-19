package com.maximum0.fastpickbe.coupon.ui.dto;

import com.maximum0.fastpickbe.coupon.domain.MyCouponStatusFilter;

public record MyCouponListRequest(
        String search,
        MyCouponStatusFilter status
) {
    public MyCouponListRequest {
        if (status == null) {
            status = MyCouponStatusFilter.ALL;
        }
    }
}
