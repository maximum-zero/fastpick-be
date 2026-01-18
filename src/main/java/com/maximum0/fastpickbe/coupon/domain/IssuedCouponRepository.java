package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.user.domain.User;

public interface IssuedCouponRepository {
    boolean existsByUserAndCoupon(User user, Coupon coupon);
    IssuedCoupon save(IssuedCoupon issuedCoupon);

    long countByCouponId(Long couponId);
    long count();
    void deleteAllInBatch();
}
