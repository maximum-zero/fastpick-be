package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CouponKeywordRepository {
    void saveAll(List<CouponKeyword> couponKeywordList);
    List<CouponSummaryResponse> findAllByCondition(CouponListRequest request, Pageable pageable, LocalDateTime now);
    Long countByCondition(CouponListRequest request, LocalDateTime now);
}
