package com.maximum0.fastpickbe.admin.coupon.application.service;

import com.maximum0.fastpickbe.coupon.application.service.CouponKeywordManager;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.CouponKeyword;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 쿠폰 관련 비즈니스 로직을 담당하는 서비스.
 *
 * - 쿠폰 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponAdminService {

    private final CouponRepository couponRepository;
    private final CouponKeywordRepository couponKeywordRepository;
    private final CouponKeywordManager couponKeywordManager;

    /**
     * 새로운 쿠폰을 생성하고, 검색 최적화를 위한 키워드 인덱스를 저장한다.
     */
    @CacheEvict(cacheNames = "coupons", allEntries = true)
    @Transactional
    public Long createCoupon(Coupon coupon) {
        Coupon savedCoupon = couponRepository.save(coupon);
        List<String> keywords = couponKeywordManager.extract(savedCoupon.getBrand(), savedCoupon.getTitle());

        if (keywords.isEmpty()) {
            log.warn("⚠️ [CouponAdminService] 키워드 추출 실패 - 기본 키워드로 저장 시도 | couponId={}", savedCoupon.getId());
        }

        List<CouponKeyword> couponKeywords = keywords.stream()
                .map(keyword -> CouponKeyword.create(savedCoupon, keyword))
                .toList();

        couponKeywordRepository.saveAll(couponKeywords);

        return savedCoupon.getId();
    }

}
