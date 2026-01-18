package com.maximum0.fastpickbe.user.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserRepositoryImpl.class, JpaConfig.class})
@DisplayName("User Repository 단위 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepositoryImpl userRepository;

    @Nested
    @DisplayName("유저 저장 및 조회 테스트")
    class SaveAndFindTest {

        @Test
        @DisplayName("유저를 저장하고 이메일로 찾을 수 있다")
        void saveAndFindByEmail_returnsUser_whenUserExists() {
            // given
            String email = "test@example.com";
            String name = "테스터";
            User user = User.create(email, "password123!", name);

            // when
            userRepository.save(user);
            Optional<User> foundUser = userRepository.findByEmail(email);

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo(email);
            assertThat(foundUser.get().getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
        void findByEmail_returnsEmpty_whenUserNotFound() {
            // when
            Optional<User> foundUser = userRepository.findByEmail("non-existent@example.com");

            // then
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("이메일 중복 확인 테스트")
    class ExistsByEmailTest {

        @Test
        @DisplayName("이미 존재하는 이메일이면 true를 반환한다")
        void existsByEmail_returnsTrue_whenEmailExists() {
            // given
            String email = "duplicate@example.com";
            userRepository.save(User.create(email, "password", "user1"));

            // when
            boolean exists = userRepository.existsByEmail(email);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 false를 반환한다")
        void existsByEmail_returnsFalse_whenEmailDoesNotExist() {
            // when
            boolean exists = userRepository.existsByEmail("new@example.com");

            // then
            assertThat(exists).isFalse();
        }
    }

}