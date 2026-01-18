package com.maximum0.fastpickbe.coupon.infra;

import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaIssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
}
