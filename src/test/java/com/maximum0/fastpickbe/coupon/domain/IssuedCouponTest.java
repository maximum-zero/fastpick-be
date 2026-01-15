package com.maximum0.fastpickbe.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("발급 쿠폰 도메인 단위 테스트")
class IssuedCouponTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 사용 테스트")
    class UseCouponTest {

        @Test
        @DisplayName("쿠폰 사용 시 사용 시점(usedAt)이 기록된다.")
        void use_SetsUsedAt_Always() {
            // given
            IssuedCoupon issuedCoupon = IssuedCoupon.create(null, null);

            // when
            issuedCoupon.use(now);

            // then
            assertThat(issuedCoupon.getUsedAt()).isEqualTo(now);
            assertThat(issuedCoupon.isUsed()).isTrue();
        }
    }

}
