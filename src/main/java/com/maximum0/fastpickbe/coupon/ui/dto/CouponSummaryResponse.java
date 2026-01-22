package com.maximum0.fastpickbe.coupon.ui.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponSummaryResponse(
        Long id,
        String brand,
        String title,
        String summary,
        int totalQuantity,
        int issuedQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endAt,
        CouponStatus status
) {
    public static CouponSummaryResponse from(Coupon coupon, LocalDateTime now) {
        return CouponSummaryResponse.builder()
                .id(coupon.getId())
                .brand(coupon.getBrand())
                .title(coupon.getTitle())
                .summary(coupon.getSummary())
                .totalQuantity(coupon.getTotalQuantity())
                .issuedQuantity(coupon.getIssuedQuantity())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .status(coupon.calculateStatus(now))
                .build();
    }
}
