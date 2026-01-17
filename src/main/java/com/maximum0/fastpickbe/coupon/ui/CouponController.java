package com.maximum0.fastpickbe.coupon.ui;

import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.CouponService;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponResponse;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    /**
     * 쿠폰 목록을 페이징하여 조회합니다.
     *
     * @param request  검색 조건
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징 처리된 쿠폰 요약 정보 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponSummaryResponse>>> getCoupons(
            @Valid CouponListRequest request,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getCoupons(request, pageable)));
    }

    /**
     * 특정 쿠폰의 상세 정보를 조회합니다.
     *
     * @param id 쿠폰 식별자
     * @return 쿠폰 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getCoupon(id)));
    }
}