package com.maximum0.fastpickbe.coupon.infra;

import com.maximum0.fastpickbe.coupon.domain.CouponKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCouponKeywordRepository extends JpaRepository<CouponKeyword, Long> {
    
}