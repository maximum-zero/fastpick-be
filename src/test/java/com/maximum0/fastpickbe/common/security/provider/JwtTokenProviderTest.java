package com.maximum0.fastpickbe.common.security.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.security.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final Clock fixedClock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private final String secretKeyStr = "vme_secret_key_for_test_at_least_32_characters_long";

    @BeforeEach
    void setUp() {
        given(jwtProperties.secretKey()).willReturn(secretKeyStr);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties, fixedClock);
        jwtTokenProvider.init();
    }

    @Nested
    @DisplayName("인증 객체 조회 테스트")
    class GetAuthentication_Behavior_Test {

        @Test
        @DisplayName("인증 객체를 통해 토큰을 생성하고, 정상적으로 복구한다")
        void getAuthentication_returnsAuthentication_fromValidToken() {
            // given
            given(jwtProperties.accessTokenExpiration()).willReturn(Duration.ofMinutes(30));
            Authentication auth = new UsernamePasswordAuthenticationToken("1", "",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            // when
            String accessToken = jwtTokenProvider.createAccessToken(auth);
            Authentication resultAuth = jwtTokenProvider.getAuthentication(accessToken);

            // then
            assertThat(resultAuth.getName()).isEqualTo("1");
            assertThat(resultAuth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("토큰에 권한 정보가 누락된 경우, INTERNAL_SERVER_ERROR를 발생시킨다")
        void getAuthentication_throwsBusinessException_whenNoAuthorities() {
            // given
            SecretKey key = Keys.hmacShaKeyFor(secretKeyStr.getBytes(StandardCharsets.UTF_8));
            String tokenWithoutAuth = Jwts.builder().subject("1").signWith(key).compact();

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tokenWithoutAuth))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("토큰 유효성 검증 테스트")
    class ValidateToken_Test {

        @Test
        @DisplayName("유효 기간 내의 정상적인 토큰인 경우, true를 반환한다")
        void validateToken_returnsTrue_whenTokenIsValid() {
            // given
            given(jwtProperties.accessTokenExpiration()).willReturn(Duration.ofMinutes(30));
            Authentication auth = new UsernamePasswordAuthenticationToken("1", "", Collections.emptyList());
            String token = jwtTokenProvider.createAccessToken(auth);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("서명이 일치하지 않는 토큰인 경우, false를 반환한다")
        void validateToken_returnsFalse_whenSignatureInvalid() {
            // given
            SecretKey wrongKey = Keys.hmacShaKeyFor("wrong_secret_key_at_least_32_chars_long".getBytes());
            String invalidToken = Jwts.builder().subject("1").signWith(wrongKey).compact();

            // when
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("JWT 형식이 올바르지 않은 경우, false를 반환한다")
        void validateToken_returnsFalse_whenTokenMalformed() {
            // given
            String malformedToken = "invalid.token.structure";

            // when
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("토큰이 null이거나 비어있는 경우, false를 반환한다")
        void validateToken_returnsFalse_whenTokenIsEmpty() {
            // when & then
            assertThat(jwtTokenProvider.validateToken(null)).isFalse();
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    class TokenGeneration_Test {

        @Test
        @DisplayName("Access Token과 Refresh Token은 서로 다른 만료 시간을 가진다")
        void createTokens_haveDifferentExpiration_always() {
            // given
            given(jwtProperties.accessTokenExpiration()).willReturn(Duration.ofMinutes(30));
            given(jwtProperties.refreshTokenExpiration()).willReturn(Duration.ofDays(7));
            Authentication auth = new UsernamePasswordAuthenticationToken("1", "", Collections.emptyList());

            // when
            String accessToken = jwtTokenProvider.createAccessToken(auth);
            String refreshToken = jwtTokenProvider.createRefreshToken(auth);

            // then
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

}
