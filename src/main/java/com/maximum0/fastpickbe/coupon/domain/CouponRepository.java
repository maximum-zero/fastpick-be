package com.maximum0.fastpickbe.coupon.domain;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findActiveById(Long id);
    Optional<Coupon> findByIdWithLock(Long id);
    List<Coupon> findAllByIds(List<Long> ids);

    void deleteAllInBatch();
}
