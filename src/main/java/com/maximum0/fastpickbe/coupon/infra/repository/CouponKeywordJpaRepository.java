package com.maximum0.fastpickbe.coupon.infra.repository;

import com.maximum0.fastpickbe.coupon.domain.model.CouponKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponKeywordJpaRepository extends JpaRepository<CouponKeyword, Long> {
    
}