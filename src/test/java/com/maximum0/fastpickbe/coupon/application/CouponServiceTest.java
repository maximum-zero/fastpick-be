package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.CouponKeywordRepository;
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
    @DisplayName("쿠폰 목록 조회 테스트")
    class GetCouponsTest {
        @Test
        @DisplayName("검색 조건이 주어지면 키워드 인덱스에서 ID를 추출하고 본체를 조회하여 정렬된 결과를 반환한다")
        void getCoupons_returnsCouponSummaries_whenCalledWithValidRequest() {
            // given
            CouponListRequest request = new CouponListRequest(null, CouponFilterType.ALL);
            Pageable pageable = PageRequest.of(0, 10);
            List<Long> couponIds = List.of(1L, 2L);

            CouponSummaryResponse response1 = new CouponSummaryResponse(1L, "나이키", "나이키 1", "요약", 100, 0, now, now.plusDays(1), CouponStatus.ISSUING.name());
            CouponSummaryResponse response2 = new CouponSummaryResponse(2L, "아디다스", "아디다스 1", "요약", 100, 0, now, now.plusDays(1), CouponStatus.ISSUING.name());

            given(couponKeywordRepository.countByCondition(request, now))
                    .willReturn(2L);

            given(couponKeywordRepository.findAllByCondition(request, pageable, now))
                    .willReturn(List.of(response1, response2));

            // when
            Page<CouponSummaryResponse> result = couponService.getCoupons(request, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(1).id()).isEqualTo(2L);

            verify(couponKeywordRepository).countByCondition(request, now);
            verify(couponKeywordRepository).findAllByCondition(request, pageable, now);
        }

        @Test
        @DisplayName("검색 결과가 없는 경우 빈 페이지를 반환하고 본체 조회를 수행하지 않는다")
        void getCoupons_ReturnsEmptyPage_WhenNoResultsFound() {
            // given
            CouponListRequest request = new CouponListRequest("없는쿠폰", CouponFilterType.ALL);
            Pageable pageable = PageRequest.of(0, 10);

            given(couponKeywordRepository.countByCondition(eq(request), eq(now)))
                    .willReturn(0L);

            // when
            Page<CouponSummaryResponse> result = couponService.getCoupons(request, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verifyNoInteractions(couponRepository);
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
            Coupon coupon = Coupon.forTest(couponId, "브랜드명", title, "요약 설명", "상세 설명", 100, 0, now.minusDays(1), now.plusDays(1));
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