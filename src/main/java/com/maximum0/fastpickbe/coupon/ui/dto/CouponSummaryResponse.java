package com.maximum0.fastpickbe.coupon.ui.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponSummaryResponse(
        Long id,
        String title,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endAt,
        CouponStatus status
) {
    public static CouponSummaryResponse from(Coupon coupon, LocalDateTime now) {
        return CouponSummaryResponse.builder()
                .id(coupon.getId())
                .title(coupon.getTitle())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .status(coupon.calculateStatus(now))
                .build();
    }
}
