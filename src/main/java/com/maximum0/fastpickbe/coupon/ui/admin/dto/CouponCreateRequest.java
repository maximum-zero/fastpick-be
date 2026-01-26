package com.maximum0.fastpickbe.coupon.ui.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CouponCreateRequest(
        @NotBlank(message = "브랜드명은 필수입니다.")
        String brand,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "요약 설명은 필수입니다.")
        String summary,

        @NotBlank(message = "상세 설명은 필수입니다.")
        String description,

        @Min(value = 1, message = "총 발행 수량은 1개 이상이어야 합니다.")
        int totalQuantity,

        @NotNull(message = "시작일은 필수입니다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startAt,

        @NotNull(message = "종료일은 필수입니다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endAt
) {
    public Coupon toEntity() {
        return Coupon.create(
                brand,
                title,
                summary,
                description,
                totalQuantity,
                startAt,
                endAt
        );
    }
}