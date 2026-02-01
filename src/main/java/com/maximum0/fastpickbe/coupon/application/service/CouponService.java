package com.maximum0.fastpickbe.coupon.application.service;

import com.maximum0.fastpickbe.common.dto.PageResponse;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponResponse;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 관련 비즈니스 로직을 담당하는 서비스.
 *
 * - 검색 조건 기반 쿠폰 목록 조회
 * - 쿠폰 단건 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponKeywordRepository couponKeywordRepository;
    private final CouponRepository couponRepository;
    private final Clock clock;

    /**
     * 검색 조건 및 현재 시점을 기준으로 활성 상태의 쿠폰 목록을 페이징 조회한다.
     */
    @Cacheable(cacheNames = "coupons", key = "#request.toString() + #pageable.pageNumber")
    public PageResponse<CouponSummaryResponse> getCoupons(CouponListRequest request, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(clock);

        long totalCount = couponKeywordRepository.countByCondition(request, now);
        if (totalCount == 0) {
            return PageResponse.from(Page.empty(pageable));
        }

        List<CouponSummaryResponse> responses = couponKeywordRepository.findAllByCondition(request, pageable, now);
        return PageResponse.from(new PageImpl<>(responses, pageable, totalCount));
    }

    /**
     * 현재 시점을 기준으로 활성 상태인 특정 쿠폰의 상세 정보를 조회한다.
     *
     * @throws BusinessException 쿠폰이 존재하지 않거나 비활성화된 경우
     */
    public CouponResponse getCoupon(Long id) {
        LocalDateTime now = LocalDateTime.now(clock);
        return couponRepository.findActiveById(id)
                .map(c -> CouponResponse.from(c, now))
                .orElseThrow(() -> {
                    log.info("ℹ️ [CouponService] 쿠폰 조회 실패 - 존재하지 않음 | couponId={}", id);
                    return new BusinessException(ErrorCode.COUPON_NOT_FOUND);
                });
    }

}
