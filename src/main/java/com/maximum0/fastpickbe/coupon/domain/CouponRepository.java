package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findActiveById(Long id);
    Optional<Coupon> findByIdWithLock(Long id);
    Page<CouponSummaryResponse> findAll(CouponListRequest request, Pageable pageable, LocalDateTime now);

    void deleteAllInBatch();
}
