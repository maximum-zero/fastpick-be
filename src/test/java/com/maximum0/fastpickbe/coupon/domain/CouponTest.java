package com.maximum0.fastpickbe.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("쿠폰 도메인 단위 테스트")
class CouponTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 상태 계산 테스트")
    class CalculateStatusTest {

        @Test
        @DisplayName("현재 시간이 시작 시간 전이면 READY 상태를 반환한다.")
        void calculateStatus_returnsReady_whenBeforeStartAt() {
            // given
            Coupon coupon = Coupon.create("발급 대기 쿠폰", 100, now.plusDays(1), now.plusDays(2));

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.READY);
        }

        @Test
        @DisplayName("발급 기간 내이고 수량이 남았으면 ISSUING 상태를 반환한다.")
        void calculateStatus_returnsIssuing_whenWithinPeriodAndHasQuantity() {
            // given
            Coupon coupon = Coupon.create("발급 중인 쿠폰", 100, now.minusDays(1), now.plusDays(1));

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.ISSUING);
        }

        @Test
        @DisplayName("수량이 모두 소진되면 EXHAUSTED 상태를 반환한다.")
        void calculateStatus_returnsExhausted_whenSoldOut() {
            // given
            Coupon coupon = Coupon.create("소진된 쿠폰", 100, 100, now.minusDays(1), now.plusDays(1));

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.EXHAUSTED);
        }

        @Test
        @DisplayName("종료 시간이 지나면 EXPIRED 상태를 반환한다.")
        void calculateStatus_returnsExpired_whenAfterEndAt() {
            // given
            Coupon coupon = Coupon.create("만료된 쿠폰", 100, now.minusDays(2), now.minusDays(1));

            // when
            CouponStatus status = coupon.calculateStatus(now);

            // then
            assertThat(status).isEqualTo(CouponStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 테스트")
    class IssueTest {

        @Test
        @DisplayName("정상 조건에서 발급 시 발급 수량이 1 증가한다.")
        void issue_increasesIssuedQuantity_whenConditionsAreMet() {
            // given
            Coupon coupon = Coupon.create("발급 중인 쿠폰", 100, now.minusDays(1), now.plusDays(1));
            int initialQuantity = coupon.getIssuedQuantity();

            // when
            coupon.issue(now);

            // then
            assertThat(coupon.getIssuedQuantity()).isEqualTo(initialQuantity + 1);
        }

        @Test
        @DisplayName("수량이 소진된 쿠폰을 발급하면 COUPON_EXHAUSTED 예외를 던진다.")
        void issue_throwsBusinessException_whenCouponIsExhausted() {
            // given
            Coupon coupon = Coupon.create("소진된 쿠폰", 100, 100, now.minusDays(1), now.plusDays(1));

            // when & then
            assertThatThrownBy(() -> coupon.issue(now))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);
        }

        @Test
        @DisplayName("발급 기간이 아닌 쿠폰을 발급하면 COUPON_NOT_AVAILABLE_PERIOD 예외를 던진다.")
        void issue_throwsBusinessException_whenOutsideOfPeriod() {
            // given
            Coupon coupon = Coupon.create("할인 쿠폰", 100, now.plusDays(1), now.plusDays(2));

            // when & then
            assertThatThrownBy(() -> coupon.issue(now))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE_PERIOD);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 상태 테스트")
    class UseStatusTest {
        @Test
        @DisplayName("쿠폰을 중단 처리하면 상태가 DISABLED로 변경된다.")
        void disable_changesUseStatus_toDisabled_whenCalled() {
            // given
            Coupon coupon = Coupon.create("할인 쿠폰", 100, now.minusDays(1), now.plusDays(1));

            // when
            coupon.disable();

            // then
            assertThat(coupon.getUseStatus()).isEqualTo(CouponUseStatus.DISABLED);
        }

        @Test
        @DisplayName("새로 생성된 쿠폰의 사용 상태는 AVAILABLE이다.")
        void getUseStatus_isAvailable_whenCouponIsNew() {
            // given
            Coupon newCoupon = Coupon.create("새 쿠폰", 100, now.minusDays(1), now.plusDays(1));

            // when & then
            assertThat(newCoupon.getUseStatus()).isEqualTo(CouponUseStatus.AVAILABLE);
        }

        @Test
        @DisplayName("쿠폰이 중단 처리되면 사용 상태는 DISABLED이다.")
        void getUseStatus_isForbidden_whenCouponIsDisabled() {
            // given
            Coupon disabledCoupon = Coupon.create("중단 쿠폰", 100, now.minusDays(1), now.plusDays(1));
            disabledCoupon.disable();

            // when & then
            assertThat(disabledCoupon.getUseStatus()).isEqualTo(CouponUseStatus.DISABLED);
        }
    }

}