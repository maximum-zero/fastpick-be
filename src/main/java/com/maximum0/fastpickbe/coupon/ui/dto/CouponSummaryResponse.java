package com.maximum0.fastpickbe.coupon.ui.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        String status
) {
    public CouponStatus getStatus() {
        return CouponStatus.valueOf(this.status);
    }
}
