package com.maximum0.fastpickbe.coupon.ui.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MyCouponResponse(
        Long id,
        Long couponId,
        String brand,
        String title,
        String summary,
        int totalQuantity,
        int issuedQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expireAt,
        MyCouponStatus status
) {
        public static MyCouponResponse from(IssuedCoupon issuedCoupon, LocalDateTime now) {
                Coupon coupon = issuedCoupon.getCoupon();
                return MyCouponResponse.builder()
                        .id(issuedCoupon.getId())
                        .couponId(coupon.getId())
                        .brand(coupon.getBrand())
                        .title(coupon.getTitle())
                        .summary(coupon.getSummary())
                        .totalQuantity(coupon.getTotalQuantity())
                        .issuedQuantity(coupon.getIssuedQuantity())
                        .expireAt(coupon.getEndAt())
                        .status(issuedCoupon.calculateStatus(now))
                        .build();
        }
}
