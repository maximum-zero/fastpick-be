package com.maximum0.fastpickbe.coupon.domain.repository;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssuedCouponRepository {
    boolean existsByUserAndCoupon(User user, Coupon coupon);
    IssuedCoupon save(IssuedCoupon issuedCoupon);
    long countByCouponId(Long couponId);
    long count();
    void deleteAllInBatch();
    Page<IssuedCoupon> findAllByUser(User user, MyCouponListRequest request, Pageable pageable, LocalDateTime now);
}
