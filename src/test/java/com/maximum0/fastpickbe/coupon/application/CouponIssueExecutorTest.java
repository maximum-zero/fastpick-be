package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.infra.CouponRepositoryImpl;
import com.maximum0.fastpickbe.coupon.infra.IssuedCouponRepositoryImpl;
import com.maximum0.fastpickbe.user.domain.User;
import com.maximum0.fastpickbe.user.domain.UserRepository;
import com.maximum0.fastpickbe.user.infra.UserRepositoryImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
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
@Import({ CouponIssueExecutor.class, CouponRepositoryImpl.class, IssuedCouponRepositoryImpl.class, UserRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CouponIssueExecutor 단위 테스트")
class CouponIssueExecutorTest {
    @Autowired
    private CouponIssueExecutor couponIssueExecutor;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        testUser = userRepository.saveAndFlush(User.create("test@test.com", "password", "테스터"));
    }

    @Nested
    @DisplayName("쿠폰 발급 실행 테스트")
    class ExecuteIssueTest {

        @Test
        @DisplayName("유효한 쿠폰인 경우 재고를 차감하고 발급 이력을 저장한다")
        void executeIssue_Succeeds_WhenConditionsAreMet() {
            // given
            Coupon coupon = Coupon.create("브랜드", "선착순 쿠폰", "요약 설명", "상세 설명", 100, now.minusDays(1), now.plusDays(1));
            Coupon savedCoupon = couponRepository.save(coupon);

            // when
            Long issuedId = couponIssueExecutor.executeIssue(savedCoupon.getId(), testUser, now);

            // then
            assertThat(issuedId).isNotNull();

            Coupon foundCoupon = couponRepository.findActiveById(savedCoupon.getId()).orElseThrow();
            assertThat(foundCoupon.getIssuedQuantity()).isEqualTo(1);
            assertThat(issuedCouponRepository.existsByUserAndCoupon(testUser, foundCoupon)).isTrue();
        }

        @Test
        @DisplayName("이미 발급받은 유저라면 ALREADY_ISSUED_COUPON 예외가 발생한다")
        void executeIssue_ThrowsException_WhenAlreadyIssued() {
            // given
            Coupon coupon = Coupon.create("브랜드", "중복 체크 쿠폰", "요약", "상세", 100, now.minusDays(1), now.plusDays(1));
            Coupon savedCoupon = couponRepository.save(coupon);

            // 이미 한 번 발급된 상태 시뮬레이션
            issuedCouponRepository.save(IssuedCoupon.create(testUser, savedCoupon));

            // when & then
            assertThatThrownBy(() -> couponIssueExecutor.executeIssue(savedCoupon.getId(), testUser, now))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ISSUED_COUPON);
        }

        @Test
        @DisplayName("쿠폰 수량이 소진되었다면 발급 시 예외가 발생한다")
        void executeIssue_ThrowsException_WhenSoldOut() {
            // given
            Coupon coupon = Coupon.create("브랜드", "마지막 쿠폰", "요약", "상세", 1, now.minusDays(1), now.plusDays(1));
            Coupon savedCoupon = couponRepository.save(coupon);

            savedCoupon.issue(now);
            couponRepository.save(savedCoupon);

            // when & then
            assertThatThrownBy(() -> couponIssueExecutor.executeIssue(savedCoupon.getId(), testUser, now))
                    .isInstanceOf(BusinessException.class);
        }
    }

}