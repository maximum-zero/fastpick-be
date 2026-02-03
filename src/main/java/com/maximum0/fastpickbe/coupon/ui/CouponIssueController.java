package com.maximum0.fastpickbe.coupon.ui;

import com.maximum0.fastpickbe.common.annotation.LoginUser;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.service.CouponIssueService;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponIssueRequest;
import com.maximum0.fastpickbe.user.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
     * 특정 쿠폰에 대한 발급 요청을 비동기적으로 접수한다
     *
     * @param request 발급 요청 정보 (쿠폰 ID)
     * @param user    현재 로그인된 사용자 정보 (@LoginUser)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> issue(@RequestBody @Valid CouponIssueRequest request, @LoginUser User user) {
        couponIssueService.issue(request.couponId(), user.getId());
        return ResponseEntity.accepted()
                .body(ApiResponse.ok(null));
    }

}
