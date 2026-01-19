package com.maximum0.fastpickbe.coupon.ui;

import com.maximum0.fastpickbe.common.annotation.LoginUser;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.MyCouponService;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자의 보유 쿠폰 관련 API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/v1/my/coupons")
@RequiredArgsConstructor
public class MyCouponController {

    private final MyCouponService myCouponService;

    /**
     * 현재 로그인된 사용자의 쿠폰 목록을 조회합니다.
     *
     * @param user     현재 로그인된 사용자
     * @param request  검색 조건
     * @param pageable 페이징 정보
     * @return 페이징된 내 쿠폰 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MyCouponResponse>>> getMyCoupons(
            @LoginUser User user,
            @Valid MyCouponListRequest request,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(myCouponService.getMyCoupons(user, request, pageable)));
    }
}
