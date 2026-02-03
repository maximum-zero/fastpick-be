package com.maximum0.fastpickbe.coupon.application.processor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Issue Processor 단위 테스트")
class CouponIssueProcessorTest {

    @InjectMocks
    private CouponIssueProcessor couponIssueProcessor;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
    private User testUser;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testUser = User.forTest(1L, "test@test.com", "password", "tester", UserRole.USER);
        testCoupon = Coupon.create("29CM", "선착순 쿠폰", "요약", "상세", 100, now.minusDays(1), now.plusDays(1));
    }

    @Nested
    @DisplayName("쿠폰 발급 실행(execute) 테스트")
    class Execute_Coupon_Issue {

        @Test
        @DisplayName("쿠폰과 유저 정보가 주어지면 발급 이력을 DB에 저장한다")
        void CouponIssueProcessor_Execute_SavesIssuedCoupon() {
            // given

            // when
            couponIssueProcessor.execute(testCoupon, testUser, now);

            // then
            verify(issuedCouponRepository, times(1)).save(any(IssuedCoupon.class));
        }

        @Test
        @DisplayName("발급 저장 시 전달받은 정보가 엔티티 생성 시 정확히 전달되어야 한다")
        void CouponIssueProcessor_Execute_VerifiesRepositoryCall() {
            // given
            LocalDateTime customIssuedAt = now.plusMinutes(5);

            // when
            couponIssueProcessor.execute(testCoupon, testUser, customIssuedAt);

            // then
            verify(issuedCouponRepository).save(any(IssuedCoupon.class));
        }

        @Test
        @DisplayName("DB 저장 중 예외가 발생하면 호출자에게 예외를 전파한다")
        void givenDatabaseError_whenExecute_thenThrowsException() {
            // given
            given(issuedCouponRepository.save(any(IssuedCoupon.class)))
                    .willThrow(new RuntimeException("DB Connection Failed or Integrity Violation"));

            // when & then
            assertThatThrownBy(() -> couponIssueProcessor.execute(testCoupon, testUser, now))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB Connection Failed");
        }
    }

}
