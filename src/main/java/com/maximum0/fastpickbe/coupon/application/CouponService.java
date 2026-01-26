package com.maximum0.fastpickbe.coupon.application;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponResponse;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {
    private final CouponKeywordRepository couponKeywordRepository;
    private final CouponRepository couponRepository;
    private final Clock clock;

    /**
     * 검색 조건에 따른 쿠폰 목록을 페이징하여 조회합니다.
     *
     * @param request  검색 필터 및 조건
     * @param pageable 페이징 설정
     * @return Page<CouponSummaryResponse> 페이징된 쿠폰 요약 정보
     */
    public Page<CouponSummaryResponse> getCoupons(CouponListRequest request, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(clock);

        long total = couponKeywordRepository.countByCondition(request, now);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<CouponSummaryResponse> responses = couponKeywordRepository.findAllByCondition(request, pageable, now);
        return new PageImpl<>(responses, pageable, total);
    }

    /**
     * 특정 쿠폰의 상세 정보를 조회합니다.
     *
     * @param id 쿠폰 식별자
     * @return CouponResponse 쿠폰 상세 정보
     */
    public CouponResponse getCoupon(Long id) {
        LocalDateTime now = LocalDateTime.now(clock);
        return couponRepository.findActiveById(id)
                .map(c -> CouponResponse.from(c, now))
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }
}
