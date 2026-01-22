package com.maximum0.fastpickbe.coupon.application;

import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 보유 쿠폰 조회 관련 비즈니스 로직을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCouponService {
    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    /**
     * 현재 로그인된 사용자의 쿠폰 목록을 검색 조건에 따라 페이징하여 조회합니다.
     *
     * @param user     현재 로그인된 사용자
     * @param request  검색 조건 (제목, 상태)
     * @param pageable 페이징 정보
     * @return 페이징된 내 쿠폰 응답 DTO 목록
     */
    public Page<MyCouponResponse> getMyCoupons(User user, MyCouponListRequest request, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(clock);
        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllByUser(user, request, pageable, now);

        return issuedCoupons.map(ic -> MyCouponResponse.from(ic, now));
    }
}
