package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CouponKeywordRepository {
    void saveAll(List<CouponKeyword> couponKeywordList);
    List<Long> findIdsByCondition(CouponListRequest request, Pageable pageable, LocalDateTime now);
    Long countByCondition(CouponListRequest request, LocalDateTime now);
}
