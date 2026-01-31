package com.maximum0.fastpickbe.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken 도메인 단위 테스트")
class RefreshTokenTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("토큰 만료 시나리오 테스트")
    class RefreshToken_Expiry_Scenario {

        @Test
        @DisplayName("현재 시간이 만료 시간 이전이면 FALSE를 반환한다")
        void givenBeforeExpiry_whenCheckExpired_thenReturnsFalse() {
            // given
            LocalDateTime expiryAt = now.plusDays(7);
            RefreshToken refreshToken = createRefreshToken(expiryAt);

            // when
            boolean result = refreshToken.isExpired(now);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("현재 시간이 만료 시간 이후이면 TRUE를 반환한다")
        void givenAfterExpiry_whenCheckExpired_thenReturnsTrue() {
            // given
            LocalDateTime expiryAt = now.minusMinutes(1);
            RefreshToken refreshToken = createRefreshToken(expiryAt);

            // when
            boolean result = refreshToken.isExpired(now);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 갱신 시나리오 테스트")
    class RefreshToken_Update_Scenario {

        @Test
        @DisplayName("새로운 토큰과 만료 시간으로 정보가 갱신된다")
        void givenNewTokenInfo_whenUpdate_thenChangesTokenAndExpiryAt() {
            // given
            RefreshToken refreshToken = createRefreshToken(now.plusDays(1));
            String newToken = "new-token";
            LocalDateTime newExpiryAt = now.plusDays(7);

            // when
            refreshToken.update(newToken, newExpiryAt);

            // then
            assertAll(
                    () -> assertThat(refreshToken.getToken()).isEqualTo(newToken),
                    () -> assertThat(refreshToken.getExpiryAt()).isEqualTo(newExpiryAt)
            );
        }
    }

    // --- 테스트 메서드 ---

    private RefreshToken createRefreshToken(LocalDateTime expiryAt) {
        return RefreshToken.forTest(1L, "token", null, expiryAt);
    }

}
