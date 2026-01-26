package com.maximum0.fastpickbe.coupon.application.admin;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponKeyword;
import com.maximum0.fastpickbe.coupon.domain.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponAdminService {
    private final CouponRepository couponRepository;
    private final CouponKeywordRepository couponKeywordRepository;
    private final KeywordExtractor keywordExtractor;

    /**
     * 쿠폰을 생성하고 검색 최적화를 위한 키워드 인덱스를 저장합니다.
     * @param coupon 생성할 쿠폰 엔티티
     * @return 생성된 쿠폰 식별자
     */
    @Transactional
    public Long createCoupon(Coupon coupon) {
        Coupon savedCoupon = couponRepository.save(coupon);
        List<String> keywords = keywordExtractor.extract(savedCoupon.getBrand(), savedCoupon.getTitle());

        List<CouponKeyword> couponKeywords = keywords.stream()
                .map(keyword -> CouponKeyword.create(savedCoupon, keyword))
                .toList();

        couponKeywordRepository.saveAll(couponKeywords);

        return savedCoupon.getId();
    }

}
