package com.maximum0.fastpickbe.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 단위 테스트")
class AuthServiceTest {
    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("회원가입 테스트")
    class SignUpTest {
        @Test
        @DisplayName("정상적인 정보로 가입하면 유저 ID를 반환한다.")
        void signUp_Success() {
            // given
            SignUpRequest request = new SignUpRequest("test@test.com", "password123", "테스터");
            User user = User.create(request.email(), request.password(), request.name());
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(user);

            // when
            Long userId = authService.signUp(request);

            // then
            assertThat(userId).isEqualTo(1L);
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
        @DisplayName("이메일과 비밀번호가 일치하면 유저 ID를 반환한다.")
        void login_Success() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User user = User.create(request.email(), request.password(), "테스터");
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

            // when
            Long userId = authService.login(request);

            // then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 LOGIN_FAILED 예외를 던진다.")
        void login_ThrowsException_WhenEmailNotFound() {
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
        void login_ThrowsException_WhenPasswordMismatch() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");
            User user = User.create(request.email(), "password123", "테스터");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
        }
    }

}
