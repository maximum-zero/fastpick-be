package com.maximum0.fastpickbe.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.auth.ui.dto.AuthResponse;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.security.provider.JwtTokenProvider;
import com.maximum0.fastpickbe.user.domain.User;
import com.maximum0.fastpickbe.user.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {
    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("회원가입 테스트")
    class SignUpTest {
        @Test
        @DisplayName("정상적인 정보로 가입하면 토큰과 유저 정보를 반환한다.")
        void signUp_savesUserAndReturnsAuthResponse_whenRequestIsValid() {
            // given
            SignUpRequest request = new SignUpRequest("test@test.com", "password123", "테스터");
            String encodedPassword = "encodedPassword";
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            User user = User.forTest(1L, request.email(), encodedPassword, request.name());

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(user);
            given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn(accessToken);
            given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn(refreshToken);

            // when
            AuthResponse response = authService.signUp(request);

            // then
            assertThat(response.user()).isNotNull();
            assertThat(response.user().id()).isEqualTo(1L);
            assertThat(response.user().email()).isEqualTo("test@test.com");
            assertThat(response.user().name()).isEqualTo("테스터");
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("이미 가입된 이메일로 가입하면 DUPLICATE_EMAIL 예외를 던진다.")
        void signUp_ThrowsException_WhenEmailAlreadyExists() {
            // given
            SignUpRequest request = new SignUpRequest("duplicate@test.com", "password123", "테스터");
            given(userRepository.existsByEmail(request.email())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 토큰과 유저 정보를 반환한다.")
        void login_returnsTokenAndUserInfo_whenCredentialsAreValid() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User user = User.forTest(1L, request.email(), "encodedPassword", "테스터");
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn(accessToken);
            given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn(refreshToken);

            // when
            AuthResponse response = authService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
            assertThat(response.grantType()).isEqualTo("Bearer");
            assertThat(response.user().id()).isEqualTo(1L);
            assertThat(response.user().email()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 LOGIN_FAILED 예외를 던진다.")
        void login_throwsBusinessException_whenLoginFailed() {
            // given
            LoginRequest request = new LoginRequest("wrong@test.com", "password123");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED 예외를 던진다.")
        void login_throwsBusinessException_whenPasswordMismatch() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");
            User user = User.forTest(1L, request.email(), "encodedPassword", "테스터");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
        }
    }

}
