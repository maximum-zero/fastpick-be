package com.maximum0.fastpickbe.coupon.ui.admin;

import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.admin.CouponAdminService;
import com.maximum0.fastpickbe.coupon.ui.admin.dto.CouponCreateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class CouponAdminController {
    private final CouponAdminService couponAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        Long couponId = couponAdminService.createCoupon(request.toEntity());
        ApiResponse<Long> response = ApiResponse.ok(couponId);

        return ResponseEntity.created(URI.create("/api/v1/admin/coupons/" + couponId))
                .body(response);
    }
}