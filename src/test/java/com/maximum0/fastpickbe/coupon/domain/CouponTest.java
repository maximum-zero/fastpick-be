package com.maximum0.fastpickbe.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponStatus;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Coupon 도메인 단위 테스트")
class CouponTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 상태 시나리오 테스트")
    class Coupon_Status_Scenario {

        @Test
        @DisplayName("시작 시간 전이면 READY 상태를 반환한다")
        void givenBeforeStart_whenCalculate_thenReturnsReady() {
            // given
            Coupon coupon = createCoupon(now.plusDays(1), now.plusDays(2));

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.READY);
        }

        @Test
        @DisplayName("발급 기간 내이고 수량이 남았으면 ISSUING 상태를 반환한다")
        void givenWithinPeriodAndHasQuantity_whenCalculate_thenReturnsIssuing() {
            // given
            Coupon coupon = createCoupon(100, 0);

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.ISSUING);
        }

        @Test
        @DisplayName("수량이 모두 소진되면 EXHAUSTED 상태를 반환한다")
        void givenSoldOut_whenCalculate_thenReturnsExhausted() {
            // given
            Coupon coupon = createCoupon(100, 100);

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.EXHAUSTED);
        }

        @Test
        @DisplayName("종료 시간이 지나면 EXPIRED 상태를 반환한다")
        void givenAfterEndTime_whenCalculate_thenReturnsExpired() {
            // given
            Coupon coupon = createCoupon(now.minusDays(2), now.minusDays(1));

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 시나리오 테스트")
    class Coupon_Issue_Scenario {

        @Test
        @DisplayName("정상 조건에서 발급 시 발급 수량이 1 증가한다")
        void givenAvailableCoupon_whenIssue_thenIncreasesQuantity() {
            // given
            Coupon coupon = createCoupon(100, 0);

            // when
            coupon.issue(now);

            // then
            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("수량이 소진된 쿠폰을 발급하면 예외를 반환한다.")
        void givenSoldOut_whenIssue_thenThrowsExceptionByExhausted() {
            // given
            Coupon coupon = createCoupon(100, 100);

            // when & then
            assertThatThrownBy(() -> coupon.issue(now))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);
        }

        @Test
        @DisplayName("발급 기간이 아닌 쿠폰을 발급하면 예외를 반환한다.")
        void givenOutsideOfPeriod_whenIssue_thenThrowsExceptionByPeriod() {
            // given
            Coupon coupon = createCoupon(now.plusDays(1), now.plusDays(2));

            // when & then
            assertThatThrownBy(() -> coupon.issue(now))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE_PERIOD);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 가능 여부 시나리오 테스트")
    class Coupon_UseStatus_Scenario {
        @Test
        @DisplayName("쿠폰을 중단 처리하면 상태가 DISABLED로 변경된다")
        void givenAvailableCoupon_whenDisable_thenChangesStatusToDisabled() {
            // given
            Coupon coupon = createCoupon(100, 0);

            // when
            coupon.disable();

            // then
            assertThat(coupon.getUseStatus()).isEqualTo(CouponUseStatus.DISABLED);
        }

        @Test
        @DisplayName("새로 생성된 쿠폰의 기본 사용 상태는 AVAILABLE이다")
        void givenNewCoupon_whenCreated_thenStatusIsAvailable() {
            // given
            Coupon newCoupon = createCoupon(100, 0);

            // when & then
            assertThat(newCoupon.getUseStatus()).isEqualTo(CouponUseStatus.AVAILABLE);
        }
    }

    // --- 테스트 메서드 ---

    private Coupon createCoupon(LocalDateTime startAt, LocalDateTime endAt) {
        return createCoupon(100, 0, startAt, endAt);
    }

    private Coupon createCoupon(int total, int issued) {
        return createCoupon(total, issued, now.minusDays(1), now.plusDays(1));
    }

    private Coupon createCoupon(int total, int issued, LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.forTest(1L, "브랜드", "쿠폰", "요약", "상세",
                total, issued, startAt, endAt, CouponUseStatus.AVAILABLE);
    }

}
