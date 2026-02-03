package com.maximum0.fastpickbe.coupon.application.processor;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기열에서 추출된 쿠폰 발급 요청을 실제 DB에 저장하는 프로세서.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProcessor {

    private final IssuedCouponRepository issuedCouponRepository;

    /**
     * 개별 쿠폰 발급 이력을 DB에 저장한다.
     *
     * @param coupon 발급 대상 쿠폰
     * @param user 발급 대상 유저
     * @param issuedAt Redis 대기열 진입 시각
     *
     * [Propagation.REQUIRES_NEW]
     * - 호출부(Listener)의 루프 중 특정 한 명의 저장이 실패(예: 데이터 정합성 오류 등)해도
     * 이미 완료된 다른 유저들의 저장은 커밋되며, 이후 유저들의 처리에도 영향을 주지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Coupon coupon, User user, LocalDateTime issuedAt) {
        IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon, issuedAt);
        issuedCouponRepository.save(issuedCoupon);

        log.info("✅ [Coupon-Issue] 쿠폰 발급 완료 | userId={}, couponId={}, issuedAt={}", user.getId(), coupon.getId(), issuedAt);
    }
}