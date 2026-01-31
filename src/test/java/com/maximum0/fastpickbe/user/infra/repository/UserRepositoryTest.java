package com.maximum0.fastpickbe.user.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.user.domain.model.User;
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
@DisplayName("User Repository 슬라이스 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepositoryImpl userRepository;

    @Nested
    @DisplayName("유저 저장 및 조회 시나리오 테스트")
    class Save_And_Find_Scenario {

        @Test
        @DisplayName("유저 정보를 저장한 후, 해당 이메일로 사용자 상세 정보를 조회할 수 있다")
        void givenSavedUser_whenFindByEmail_thenReturnsUser() {
            // given
            String email = "test@example.com";
            String name = "테스터";
            User user = User.create(email, "password123!", name);
            userRepository.save(user);

            // when
            Optional<User> foundUser = userRepository.findByEmail(email);

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo(email);
            assertThat(foundUser.get().getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 이메일로 조회하면, 빈 Optional을 반환한다")
        void givenNonExistentEmail_whenFindByEmail_thenReturnsEmpty() {
            // when
            Optional<User> foundUser = userRepository.findByEmail("non-existent@example.com");

            // then
            assertThat(foundUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("이메일 중복 확인 시나리오 테스트")
    class Email_Duplication_Check_Scenario {

        @Test
        @DisplayName("시스템에 이미 존재하는 이메일이면, 중복 여부 확인 시 true를 반환한다")
        void givenExistingEmail_whenExistsByEmail_thenReturnsTrue() {
            // given
            String email = "duplicate@example.com";
            userRepository.save(User.create(email, "password", "user1"));

            // when
            boolean exists = userRepository.existsByEmail(email);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("시스템에 존재하지 않는 이메일이면, 중복 여부 확인 시 false를 반환한다")
        void givenNewEmail_whenExistsByEmail_thenReturnsFalse() {
            // when
            boolean exists = userRepository.existsByEmail("new@example.com");

            // then
            assertThat(exists).isFalse();
        }
    }

}
