package com.maximum0.fastpickbe.coupon.ui.dto;

import com.maximum0.fastpickbe.coupon.domain.vo.MyCouponStatusFilter;

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
