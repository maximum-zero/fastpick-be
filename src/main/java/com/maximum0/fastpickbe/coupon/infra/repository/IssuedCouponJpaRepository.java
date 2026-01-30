package com.maximum0.fastpickbe.coupon.infra.repository;

import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {
}
