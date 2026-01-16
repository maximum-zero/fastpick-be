package com.maximum0.fastpickbe.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("유저 도메인 단위 테스트")
class UserTest {
    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("유저 생성 테스트")
    class CreateUserTest {

        @Test
        @DisplayName("유저를 생성하면 입력한 정보가 정확히 저장되고 기본 권한은 USER가 된다.")
        void user_SetsInitialInfoAndDefaultRole_WhenCreated() {
            // given
            String email = "test@example.com";
            String password = "encoded-password";
            String name = "테스터";

            // when
            User user = User.create(email, password, name);

            // then
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(password);
            assertThat(user.getName()).isEqualTo(name);
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
        }
    }

    @Nested
    @DisplayName("비밀번호 인증 테스트")
    class AuthenticateTest {

        @Test
        @DisplayName("올바른 비밀번호를 입력하면 예외가 발생하지 않는다.")
        void authenticate_DoesNotThrowException_WhenPasswordMatches() {
            // given
            String rawPassword = "password123";
            String encodedPassword = "encodedPassword";
            User user = User.create("test@test.com", encodedPassword, "테스터");

            given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);

            // when & then
            assertThatCode(() -> user.authenticate(passwordEncoder, rawPassword))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("잘못된 비밀번호를 입력하면 LOGIN_FAILED 예외를 던진다.")
        void authenticate_ThrowsException_WhenPasswordMismatches() {
            // given
            String wrongPassword = "wrongPassword";
            User user = User.create("test@test.com", "encodedPassword", "테스터");

            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> user.authenticate(passwordEncoder, wrongPassword))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
        }
    }

}