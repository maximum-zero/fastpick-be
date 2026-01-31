package com.maximum0.fastpickbe.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.application.facade.CouponIssueFacade;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.infra.repository.CouponRepositoryImpl;
import com.maximum0.fastpickbe.coupon.infra.repository.IssuedCouponRepositoryImpl;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import com.maximum0.fastpickbe.user.infra.repository.UserRepositoryImpl;
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
@Import({ CouponIssueFacade.class, CouponRepositoryImpl.class, IssuedCouponRepositoryImpl.class, UserRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Coupon Issue Facade 슬라이스 테스트")
class CouponIssueFacadeTest {

    @Autowired
    private CouponIssueFacade couponIssueFacade;

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
    @DisplayName("쿠폰 발급 실행 시나리오 테스트")
    class Execute_Issue_Scenario {

        @Test
        @DisplayName("유효한 쿠폰 정보가 주어지고 발급을 실행하면 재고를 차감하고 발급 이력을 정상 저장한다")
        void givenValidCoupon_whenExecuteIssue_thenReturnsIssuedId() {
            // given
            Coupon savedCoupon = saveTestCoupon("선착순 쿠폰", 100);

            // when
            Long issuedId = couponIssueFacade.executeIssue(savedCoupon.getId(), testUser, now);

            // then
            assertThat(issuedId).isNotNull();

            Coupon foundCoupon = couponRepository.findActiveById(savedCoupon.getId()).orElseThrow();
            assertThat(foundCoupon.getIssuedQuantity()).isEqualTo(1);
            assertThat(issuedCouponRepository.existsByUserAndCoupon(testUser, foundCoupon)).isTrue();
        }

        @Test
        @DisplayName("이미 발급받은 유저 정보가 주어지면 발급 시 중복 발급 예외를 반환한다")
        void givenAlreadyIssuedUser_whenExecuteIssue_thenThrowsExceptionByAlreadyIssued() {
            // given
            Coupon savedCoupon = saveTestCoupon("중복 체크 쿠폰", 100);
            issuedCouponRepository.save(IssuedCoupon.create(testUser, savedCoupon));

            // when & then
            assertThatThrownBy(() -> couponIssueFacade.executeIssue(savedCoupon.getId(), testUser, now))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ISSUED_COUPON);
        }

        @Test
        @DisplayName("품절된 쿠폰 정보가 주어지면 발급 시 수량 소진 예외를 반환한다")
        void givenSoldOutCoupon_whenExecuteIssue_thenThrowsExceptionBySoldOut() {
            // given
            Coupon savedCoupon = saveTestCoupon("마지막 쿠폰", 1);
            savedCoupon.issue(now);
            couponRepository.save(savedCoupon);

            // when & then
            assertThatThrownBy(() -> couponIssueFacade.executeIssue(savedCoupon.getId(), testUser, now))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // --- 테스트 메서드 ---

    private Coupon saveTestCoupon(String title, int totalQuantity) {
        Coupon coupon = Coupon.create("브랜드", title, "요약", "상세", totalQuantity, now.minusDays(1), now.plusDays(1));
        return couponRepository.save(coupon);
    }

}
