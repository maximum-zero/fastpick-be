package com.maximum0.fastpickbe.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("리프레시 토큰 도메인 단위 테스트")
class RefreshTokenTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("리프레시 토큰 만료 여부 확인 테스트")
    class IsExpiredTest {

        @Test
        @DisplayName("현재 시간이 만료 시간 이전이면 false를 반환한다.")
        void isExpired_ReturnsFalse_WhenBeforeExpiryAt() {
            // given
            LocalDateTime expiryAt = now.plusDays(7);
            RefreshToken refreshToken = RefreshToken.create("token", null, expiryAt);

            // when
            boolean result = refreshToken.isExpired(now);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("현재 시간이 만료 시간 이후이면 true를 반환한다.")
        void isExpired_ReturnsTrue_WhenAfterExpiryAt() {
            // given
            LocalDateTime expiryAt = now.minusMinutes(1);
            RefreshToken refreshToken = RefreshToken.create("token", null, expiryAt);

            // when
            boolean result = refreshToken.isExpired(now);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 업데이트 테스트")
    class UpdateTest {

        @Test
        @DisplayName("새로운 토큰과 만료 시간으로 정보를 갱신한다.")
        void update_ChangesTokenAndExpiryAt_Always() {
            // given
            RefreshToken refreshToken = RefreshToken.create("old-token", null, now.plusDays(1));
            String newToken = "new-token";
            LocalDateTime newExpiryAt = now.plusDays(7);

            // when
            refreshToken.update(newToken, newExpiryAt);

            // then
            assertThat(refreshToken.getToken()).isEqualTo(newToken);
            assertThat(refreshToken.getExpiryAt()).isEqualTo(newExpiryAt);
        }
    }

}