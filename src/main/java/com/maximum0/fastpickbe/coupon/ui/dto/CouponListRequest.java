package com.maximum0.fastpickbe.coupon.ui.dto;

import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import jakarta.validation.constraints.Size;

public record CouponListRequest(
        @Size(max = 50, message = "검색어는 50자 이내여야 합니다.")
        String search,

        CouponFilterType filterType
) {
        public CouponListRequest {
                if (filterType == null) filterType = CouponFilterType.ALL;
        }
}
