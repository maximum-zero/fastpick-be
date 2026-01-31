package com.maximum0.fastpickbe.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.auth.ui.dto.AuthResponse;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.security.provider.JwtTokenProvider;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
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
@DisplayName("Auth Service 단위 테스트")
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
    @DisplayName("회원가입 시나리오 테스트")
    class SignUp_Scenario {
        @Test
        @DisplayName("정상적인 가입 정보가 주어지면 회원가입을 수행하고 토큰을 반환한다")
        void givenValidRequest_whenSignUp_thenReturnsAuthResponse() {
            // given
            SignUpRequest request = createSignUpRequest();
            String encodedPassword = "encodedPassword";
            User user = createTestUser(request.email(), encodedPassword);

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(user);
            given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn("refresh-token");

            // when
            AuthResponse response = authService.signUp(request);

            // then
            assertThat(response.user().email()).isEqualTo(request.email());
            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("이미 가입된 이메일이 주어지면 회원가입 중복 예외를 반환한다")
        void givenDuplicateEmail_whenSignUp_thenThrowsExceptionByDuplicateEmail() {
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
    @DisplayName("로그인 시나리오 테스트")
    class Login_Scenario {

        @Test
        @DisplayName("일치하는 인증 정보가 주어지면 로그인을 수행하고 토큰을 반환한다")
        void givenValidCredentials_whenLogin_thenReturnsAuthResponse() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User user = createTestUser(request.email(), "encodedPassword");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(any(Authentication.class))).willReturn("refresh-token");

            // when
            AuthResponse response = authService.login(request);

            // then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.user().id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인을 시도하면 로그인 실패 예외를 반환한다")
        void givenNonExistentEmail_whenLogin_thenThrowsExceptionByLoginFailed() {
            // given
            LoginRequest request = new LoginRequest("wrong@test.com", "password123");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 로그인 실패 예외를 반환한다")
        void givenWrongPassword_whenLogin_thenThrowsExceptionByLoginFailed() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");
            User user = User.forTest(1L, request.email(), "encodedPassword", "테스터", UserRole.USER);

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
        }
    }

    // --- 테스트 메서드 ---

    private SignUpRequest createSignUpRequest() {
        return new SignUpRequest("test@test.com", "password123", "테스터");
    }

    private User createTestUser(String email, String encodedPassword) {
        return User.forTest(1L, email, encodedPassword, "테스터", UserRole.USER);
    }

}
