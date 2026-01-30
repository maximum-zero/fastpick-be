package com.maximum0.fastpickbe.coupon.infra;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCouponRepository extends JpaRepository<Coupon, Long> {
}