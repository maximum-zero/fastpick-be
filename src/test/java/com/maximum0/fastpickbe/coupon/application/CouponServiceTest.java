package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponResponse;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Service 단위 테스트")
class CouponServiceTest {
    @InjectMocks
    private CouponService couponService;
    @Mock
    private CouponRepository couponRepository;

    @Mock
    private Clock clock;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 17, 10, 0);
    private final Instant fixedInstant = now.atZone(ZoneId.systemDefault()).toInstant();

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
    }

    @Nested
    @DisplayName("쿠폰 목록 조회 테스트")
    class GetCouponsTest {
        @Test
        @DisplayName("목록 조회 시 현재 시간을 기준으로 리포지토리를 호출하고, 결과를 DTO로 변환한다")
        void getCoupons_returnsCouponSummaries_whenCalledWithValidRequest() {
            // given
            CouponListRequest request = new CouponListRequest(null, CouponFilterType.ALL);
            Pageable pageable = PageRequest.of(0, 10);

            CouponSummaryResponse summary = new CouponSummaryResponse(
                    1L,
                    "발급 중 쿠폰",
                    now.minusDays(1),
                    now.plusDays(1),
                    CouponStatus.ISSUING
            );
            Page<CouponSummaryResponse> mockPage = new PageImpl<>(List.of(summary), pageable, 1);

            given(couponRepository.findAll(eq(request), eq(pageable), eq(now)))
                    .willReturn(mockPage);

            // when
            Page<CouponSummaryResponse> result = couponService.getCoupons(request, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("발급 중 쿠폰");
            verify(couponRepository).findAll(eq(request), eq(pageable), eq(now));
        }
    }

    @Nested
    @DisplayName("쿠폰 상세 조회 테스트")
    class GetCouponTest {

        @Test
        @DisplayName("존재하는 쿠폰 ID로 조회하면 상세 정보를 반환한다")
        void getCoupon_returnsCouponResponse_whenIdExists() {
            // given
            Long couponId = 1L;
            String title = "테스트 쿠폰";
            Coupon coupon = Coupon.forTest(couponId, title, 100, 0, now.minusDays(1), now.plusDays(1));
            given(couponRepository.findActiveById(couponId)).willReturn(Optional.of(coupon));

            // when
            CouponResponse response = couponService.getCoupon(couponId);

            // then
            assertThat(response.id()).isEqualTo(couponId);
            assertThat(response.title()).isEqualTo(title);
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화된 쿠폰 조회 시 COUPON_NOT_FOUND 예외를 던진다")
        void getCoupon_throwsBusinessException_whenCouponNotFound() {
            // given
            Long couponId = 999L;
            given(couponRepository.findActiveById(couponId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.getCoupon(couponId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
        }
    }
}