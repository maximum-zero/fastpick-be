package com.maximum0.fastpickbe.coupon.infra.repository.query;

import static com.maximum0.fastpickbe.coupon.domain.model.QCoupon.coupon;

import com.maximum0.fastpickbe.coupon.domain.vo.CouponStatus;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import java.time.LocalDateTime;

public abstract class CouponExpressions {

    /**
     * 쿠폰이 발행 대기 상태(시작 전)인지 확인하는 조건을 생성한다.
     *
     * @param now 기준 시각
     * @return 발행 시작 전 조건 표현식
     */
    public static BooleanExpression isReady(LocalDateTime now) {
        return coupon.startAt.after(now);
    }

    /**
     * 쿠폰이 발급 가능한 상태(ISSUING)인지 확인하는 조건을 생성한다.
     *
     * @param now 기준 시각
     * @return 발행 기간 내이며 잔여 수량이 존재하고 활성 상태인 조건 표현식
     */
    public static BooleanExpression isIssuing(LocalDateTime now) {
        return coupon.startAt.loe(now)
                .and(coupon.endAt.gt(now))
                .and(coupon.totalQuantity.gt(coupon.issuedQuantity))
                .and(coupon.useStatus.eq(CouponUseStatus.AVAILABLE));
    }

    /**
     * 쿠폰이 발행 만료 상태인지 확인하는 조건을 생성한다.
     *
     * @param now 기준 시각
     * @return 종료 시각이 지났음을 나타내는 조건 표현식
     */
    public static BooleanExpression isExpired(LocalDateTime now) {
        return coupon.endAt.loe(now);
    }

    /**
     * 쿠폰 수량이 모두 소진되었는지 확인하는 조건을 생성한다.
     *
     * @return 전체 수량보다 발행 수량이 같거나 많은 조건 표현식
     */
    public static BooleanExpression isExhausted() {
        return coupon.totalQuantity.loe(coupon.issuedQuantity);
    }

    /**
     * 관리자에 의해 발급이 중단되었는지 확인하는 조건을 생성한다.
     *
     * @return 사용 상태가 DISABLED인 조건 표현식
     */
    public static BooleanExpression isDisabled() {
        return coupon.useStatus.eq(CouponUseStatus.DISABLED);
    }

    /**
     * 현재 시각 기준 쿠폰의 상태를 결정하는 CASE 문을 생성한다.
     *
     * @param now 기준 시각
     * @return 쿠폰 상태(String)에 대한 CASE 표현식
     */
    public static StringExpression toCouponStatus(LocalDateTime now) {
        return new CaseBuilder()
                .when(isDisabled()).then(CouponStatus.DISABLED.name())
                .when(isExpired(now)).then(CouponStatus.EXPIRED.name())
                .when(isExhausted()).then(CouponStatus.EXHAUSTED.name())
                .when(isReady(now)).then(CouponStatus.READY.name())
                .otherwise(CouponStatus.ISSUING.name());
    }
}
