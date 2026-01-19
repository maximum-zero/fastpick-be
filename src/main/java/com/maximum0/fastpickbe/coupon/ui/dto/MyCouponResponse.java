package com.maximum0.fastpickbe.coupon.ui.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatus;
import java.time.LocalDateTime;

public record MyCouponResponse(
        Long id,
        Long couponId,
        String title,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expireAt,
        MyCouponStatus status
) {
}
