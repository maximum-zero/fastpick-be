package com.maximum0.fastpickbe.coupon.domain.repository;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findActiveById(Long id);
    List<Coupon> findAllByIds(List<Long> ids);

    void deleteAllInBatch();
}
