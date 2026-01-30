package com.maximum0.fastpickbe.coupon.infra.repository;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCouponRepository extends JpaRepository<Coupon, Long> {
}