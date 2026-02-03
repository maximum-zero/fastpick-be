package com.maximum0.fastpickbe.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IssuedCoupon 도메인 단위 테스트")
class IssuedCouponTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 사용 시나리오 테스트")
    class IssuedCoupon_Use_Scenario {

        @Test
        @DisplayName("쿠폰 사용 시 사용 시점이 기록되고 사용 상태가 TRUE가 된다")
        void givenIssuedCoupon_whenUse_thenSetsUsedAtAndStatusTrue() {
            // given
            IssuedCoupon issuedCoupon = createIssuedCoupon();

            // when
            issuedCoupon.use(now);

            // then
            assertThat(issuedCoupon.getUsedAt()).isEqualTo(now);
            assertThat(issuedCoupon.isUsed()).isTrue();
        }

        @Test
        @DisplayName("새로 발급된 쿠폰은 기본적으로 사용 상태가 FALSE이다")
        void givenNewIssuedCoupon_whenCreated_thenIsUsedIsFalse() {
            // given
            IssuedCoupon issuedCoupon = createIssuedCoupon();

            // when & then
            assertThat(issuedCoupon.isUsed()).isFalse();
            assertThat(issuedCoupon.getUsedAt()).isNull();
        }
    }

    // --- 테스트 메서드 ---

    private IssuedCoupon createIssuedCoupon() {
        return IssuedCoupon.forTest(1L, null, null,  null,null);
    }

}
