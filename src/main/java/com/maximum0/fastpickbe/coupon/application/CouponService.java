package com.maximum0.fastpickbe.coupon.application;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponResponse;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {
    private final CouponRepository couponRepository;
    private final Clock clock;

    public Page<CouponSummaryResponse> getCoupons(CouponListRequest request, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(clock);
        return couponRepository.findAll(request, pageable, now);
    }

    public CouponResponse getCoupon(Long id) {
        LocalDateTime now = LocalDateTime.now(clock);
        return couponRepository.findActiveById(id)
                .map(c -> CouponResponse.from(c, now))
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }
}
