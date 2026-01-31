package com.maximum0.fastpickbe.coupon.application.service;

import com.maximum0.fastpickbe.common.dto.PageResponse;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final Clock clock;

    /**
     * 특정 사용자의 쿠폰 발급 목록을 검색 조건에 따라 페이징하여 조회한다.
     * 쿠폰의 제목이나 사용 가능 상태 등을 기준으로 필터링을 수행한다.
     *
     * @param user     조회 대상 사용자 엔티티
     * @param request  검색 필터 조건 (쿠폰 제목, 상태 등)
     * @param pageable 페이징 설정 정보
     * @return 페이징 처리된 내 쿠폰 정보(MyCouponResponse) 목록
     */
    public PageResponse<MyCouponResponse> getIssuedCoupons(User user, MyCouponListRequest request, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(clock);
        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllByUser(user, request, pageable, now);

        return PageResponse.from(issuedCoupons.map(ic -> MyCouponResponse.from(ic, now)));
    }

}
