package com.maximum0.fastpickbe.coupon.domain;

import static com.maximum0.fastpickbe.coupon.domain.QCoupon.coupon;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import java.time.LocalDateTime;

public abstract class CouponExpressions {
    /**
     * 쿠폰이 발행 대기 상태(시작 전)인지 확인하는 조건을 생성합니다.
     * @param now 기준 시간
     * @return 현재 시각 < 시작 시각 조건
     */
    public static BooleanExpression isReady(LocalDateTime now) {
        return coupon.startAt.after(now);
    }

    /**
     * 쿠폰이 현재 발급 가능한 상태(ISSUING)인지 확인하는 조건을 생성합니다.
     * @param now 기준 시간
     * @return 발행 기간 내 존재 && 잔여 수량 존재 && 활성 상태 조건
     */
    public static BooleanExpression isIssuing(LocalDateTime now) {
        return coupon.startAt.loe(now)
                .and(coupon.endAt.gt(now))
                .and(coupon.totalQuantity.gt(coupon.issuedQuantity))
                .and(coupon.useStatus.eq(CouponUseStatus.AVAILABLE));
    }

    /**
     * 쿠폰이 발행 만료 상태인지 확인하는 조건을 생성합니다.
     * @param now 기준 시간
     * @return 현재 시각 >= 종료 시각 조건
     */
    public static BooleanExpression isExpired(LocalDateTime now) {
        return coupon.endAt.loe(now);
    }

    /**
     * 쿠폰이 소진(품절) 상태인지 확인하는 조건을 생성합니다.
     * @return 발행 수량 >= 전체 수량 조건
     */
    public static BooleanExpression isExhausted() {
        return coupon.totalQuantity.loe(coupon.issuedQuantity);
    }

    /**
     * 쿠폰이 관리자에 의해 발급 중단 상태인지 확인하는 조건을 생성합니다.
     * @return useStatus == DISABLED 조건
     */
    public static BooleanExpression isDisabled() {
        return coupon.useStatus.eq(CouponUseStatus.DISABLED);
    }

    /**
     * DB 조회 시 쿠폰의 상태를 동적으로 계산하는 CASE 문을 생성합니다.
     * @param now 기준 시간
     * @return 쿠폰 상태 문자열을 반환하는 StringExpression
     */
    public static StringExpression buildCouponStatus(LocalDateTime now) {
        return new CaseBuilder()
                .when(isDisabled()).then(CouponStatus.DISABLED.name())
                .when(isExpired(now)).then(CouponStatus.EXPIRED.name())
                .when(isExhausted()).then(CouponStatus.EXHAUSTED.name())
                .when(isReady(now)).then(CouponStatus.READY.name())
                .otherwise(CouponStatus.ISSUING.name());
    }
}
