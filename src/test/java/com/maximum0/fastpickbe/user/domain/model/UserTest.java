package com.maximum0.fastpickbe.user.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("User 도메인 단위 테스트")
class UserTest {
    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("유저 생성 시나리오 테스트")
    class User_Create_Scenario {

        @Test
        @DisplayName("유저 생성 시 입력 정보가 저장되며 기본 권한은 USER로 설정된다")
        void givenUserInfo_whenCreate_thenSetsInitialInfoAndDefaultRole() {
            // given
            String email = "test@example.com";
            String password = "encoded-password";
            String name = "테스터";

            // when
            User user = User.create(email, password, name);

            // then
            assertAll(
                    () -> assertThat(user.getEmail()).isEqualTo(email),
                    () -> assertThat(user.getPassword()).isEqualTo(password),
                    () -> assertThat(user.getName()).isEqualTo(name),
                    () -> assertThat(user.getRole()).isEqualTo(UserRole.USER)
            );
        }
    }

    @Nested
    @DisplayName("비밀번호 인증 시나리오 테스트")
    class User_Authenticate_Scenario {

        @Test
        @DisplayName("비밀번호가 일치하면 예외가 발생하지 않는다")
        void givenCorrectPassword_whenAuthenticate_thenDoesNotThrowAnyException() {
            // given
            String rawPassword = "password123";
            String encodedPassword = "encodedPassword";
            User user = createUser(encodedPassword);

            given(passwordEncoder.matches(rawPassword, encodedPassword))
                    .willReturn(true);

            // when & then
            assertThatCode(() -> user.authenticate(passwordEncoder, rawPassword))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 로그인 실패 예외를 반환한다.")
        void givenWrongPassword_whenAuthenticate_thenThrowsExceptionByLoginFailed() {
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

    // --- 테스트 메서드 ---

    private User createUser(String password) {
        return User.forTest(1L, "test@test.com", password, "테스터", UserRole.USER);
    }

}