package com.maximum0.fastpickbe.coupon.ui.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CouponResponse(
        Long id,
        String brand,
        String title,
        String description,
        int totalQuantity,
        int issuedQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endAt,
        CouponStatus status
) {
    public static CouponResponse from(Coupon coupon, LocalDateTime now) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .brand(coupon.getBrand())
                .title(coupon.getTitle())
                .description(coupon.getDescription())
                .totalQuantity(coupon.getTotalQuantity())
                .issuedQuantity(coupon.getIssuedQuantity())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .status(coupon.calculateStatus(now))
                .build();
    }
}
