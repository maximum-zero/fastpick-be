package com.maximum0.fastpickbe.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maximum0.fastpickbe.common.dto.PageResponse;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponStatus;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
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
    private CouponKeywordRepository couponKeywordRepository;

    @Mock
    private Clock clock;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final Instant fixedInstant = now.atZone(ZoneId.systemDefault()).toInstant();

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
    }

    @Nested
    @DisplayName("쿠폰 목록 조회 시나리오 테스트")
    class Get_Coupons_Scenario {
        @Test
        @DisplayName("검색 조건이 주어지면 키워드 인덱스에서 정보를 추출하고 정렬된 결과를 반환한다")
        void givenValidRequest_whenGetCoupons_thenReturnsCouponSummaries() {
            // given
            CouponListRequest request = new CouponListRequest(null, CouponFilterType.ALL);
            Pageable pageable = PageRequest.of(0, 10);

            CouponSummaryResponse response1 = new CouponSummaryResponse(1L, "나이키", "나이키 1", "요약", 100, 0, now, now.plusDays(1), CouponStatus.ISSUING.name());
            CouponSummaryResponse response2 = new CouponSummaryResponse(2L, "아디다스", "아디다스 1", "요약", 100, 0, now, now.plusDays(1), CouponStatus.ISSUING.name());

            given(couponKeywordRepository.countByCondition(request, now)).willReturn(2L);
            given(couponKeywordRepository.findAllByCondition(request, pageable, now)).willReturn(List.of(response1, response2));

            // when
            PageResponse<CouponSummaryResponse> result = couponService.getCoupons(request, pageable);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).id()).isEqualTo(1L);
            verify(couponKeywordRepository, times(1)).countByCondition(request, now);
            verify(couponKeywordRepository, times(1)).findAllByCondition(request, pageable, now);
        }

        @Test
        @DisplayName("검색 결과가 없는 조건이 주어지면 빈 페이지를 반환하고 본체 조회를 수행하지 않는다")
        void givenNoResultsCondition_whenGetCoupons_thenReturnsEmptyPage() {
            // given
            CouponListRequest request = new CouponListRequest("없는쿠폰", CouponFilterType.ALL);
            Pageable pageable = PageRequest.of(0, 10);

            given(couponKeywordRepository.countByCondition(eq(request), eq(now))).willReturn(0L);

            // when
            PageResponse<CouponSummaryResponse> result = couponService.getCoupons(request, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verifyNoInteractions(couponRepository);
        }
    }

    @Nested
    @DisplayName("쿠폰 상세 조회 시나리오 테스트")
    class Get_Coupon_Scenario {

        @Test
        @DisplayName("존재하는 쿠폰 ID가 주어지면 쿠폰 상세 정보를 반환한다")
        void givenExistingId_whenGetCoupon_thenReturnsCouponResponse() {
            // given
            Long couponId = 1L;
            String title = "테스트 쿠폰";
            Coupon coupon = Coupon.forTest(couponId, "브랜드명", title, "요약 설명", "상세 설명", 100, 0, now.minusDays(1), now.plusDays(1), CouponUseStatus.AVAILABLE);
            given(couponRepository.findActiveById(couponId)).willReturn(Optional.of(coupon));

            // when
            CouponResponse response = couponService.getCoupon(couponId);

            // then
            assertThat(response.id()).isEqualTo(couponId);
            assertThat(response.title()).isEqualTo(title);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID가 주어지면 쿠폰 상세 조회 실패 예외를 반환한다")
        void givenNonExistentId_whenGetCoupon_thenThrowsExceptionByCouponNotFound() {
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
