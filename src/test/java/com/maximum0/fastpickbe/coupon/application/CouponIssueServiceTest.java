package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Issue Service 단위 테스트")
class CouponIssueServiceTest {
    @InjectMocks
    private CouponIssueService couponIssueService;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private Clock clock;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 1, 0);
    private final Instant fixedInstant = now.atZone(ZoneId.systemDefault()).toInstant();

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
    }

    @Nested
    @DisplayName("쿠폰 발급 테스트")
    class IssueCouponTest {
        @Test
        @DisplayName("모든 발급 조건이 충족되면 쿠폰을 정상적으로 발급한다")
        void issue_succeeds_whenConditionsAreMet() {
            // given
            Long couponId = 1L;
            User user = User.forTest(1L, "test@test.com", "pw", "테스터");
            Coupon coupon = Coupon.forTest(couponId, "브랜드명", "선착순 쿠폰", "요약 설명", "상세 설명", 100, 0, now.minusDays(1), now.plusDays(1));

            IssuedCoupon mockSavedIssuedCoupon = mock(IssuedCoupon.class);

            given(couponRepository.findByIdWithLock(couponId)).willReturn(Optional.of(coupon));
            given(issuedCouponRepository.existsByUserAndCoupon(user, coupon)).willReturn(false);
            given(issuedCouponRepository.save(any(IssuedCoupon.class))).willReturn(mockSavedIssuedCoupon);
            given(mockSavedIssuedCoupon.getId()).willReturn(100L);

            // when
            Long issuedId = couponIssueService.issue(couponId, user);

            // then
            assertThat(issuedId).isEqualTo(100L);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
            verify(issuedCouponRepository).save(any(IssuedCoupon.class));
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰이면 중복 발급 예외가 발생한다")
        void issue_throwsBusinessException_whenCouponIsAlreadyIssued() {
            // given
            Long couponId = 1L;
            User user = User.forTest(1L, "test@test.com", "pw", "테스터");
            Coupon coupon = Coupon.forTest(couponId, "브랜드명", "중복 쿠폰", "요약 설명", "상세 설명", 100, 0, now.minusDays(1), now.plusDays(1));

            given(couponRepository.findByIdWithLock(couponId)).willReturn(Optional.of(coupon));
            given(issuedCouponRepository.existsByUserAndCoupon(user, coupon)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(couponId, user))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ISSUED_COUPON);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 발급하려 하면 예외가 발생한다")
        void issue_throwsBusinessException_whenCouponNotFound() {
            // given
            Long couponId = 999L;
            User user = User.forTest(1L, "test@test.com", "pw", "테스터");

            given(couponRepository.findByIdWithLock(couponId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(couponId, user))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
        }
    }
}