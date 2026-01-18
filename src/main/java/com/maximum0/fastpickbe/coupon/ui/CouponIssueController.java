package com.maximum0.fastpickbe.coupon.ui;

import com.maximum0.fastpickbe.common.annotation.LoginUser;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.CouponIssueService;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponIssueRequest;
import com.maximum0.fastpickbe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupon-issues")
@RequiredArgsConstructor
public class CouponIssueController {
    private final CouponIssueService couponIssueService;

    /**
     * 특정 쿠폰에 대한 발급 요청을 처리합니다.
     * 
     * @param request 발급 요청 정보 (쿠폰 ID 등)
     * @param user    @LoginUser 어노테이션을 통해 주입된 현재 인증된 사용자
     * @return ApiResponse<Long> 생성된 발급 이력의 식별자
     */
    @PostMapping
    public ApiResponse<Long> issue(@RequestBody @Valid CouponIssueRequest request, @LoginUser User user) {
        Long issuedId = couponIssueService.issue(request.couponId(), user);
        return ApiResponse.ok(issuedId);
    }
}