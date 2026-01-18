package com.maximum0.fastpickbe.coupon.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({IssuedCouponRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class})
@DisplayName("IssuedCoupon Repository 단위 테스트")
class IssuedCouponRepositoryTest {
    @Autowired
    private IssuedCouponRepositoryImpl issuedCouponRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 1, 0);

    @Nested
    @DisplayName("중복 발급 체크 테스트")
    class DuplicationCheckTest {

        private User user;
        private Coupon coupon;

        @BeforeEach
        void setUp() {
            user = User.create("test@test.com", "encodedPassword", "테스터");
            entityManager.persist(user);

            coupon = Coupon.create("테스트쿠폰", 100, now, now.plusDays(1));
            entityManager.persist(coupon);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("유저가 이미 쿠폰을 발급받았다면 true를 반환한다")
        void existsByUserAndCoupon_returnsTrue_whenRecordExists() {
            // given
            IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon);
            issuedCouponRepository.save(issuedCoupon);
            entityManager.flush();

            // when
            boolean exists = issuedCouponRepository.existsByUserAndCoupon(user, coupon);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("발급 이력이 없다면 false를 반환한다")
        void existsByUserAndCoupon_returnsFalse_whenRecordNotExists() {
            // when
            boolean exists = issuedCouponRepository.existsByUserAndCoupon(user, coupon);

            // then
            assertThat(exists).isFalse();
        }
    }
}
