package com.maximum0.fastpickbe.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("유저 도메인 단위 테스트")
class UserTest {

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

}