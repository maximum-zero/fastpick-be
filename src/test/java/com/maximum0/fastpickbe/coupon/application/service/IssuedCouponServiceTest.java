package com.maximum0.fastpickbe.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.common.dto.PageResponse;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("Issued Coupon Service 단위 테스트")
class IssuedCouponServiceTest {

    @InjectMocks
    private IssuedCouponService issuedCouponService;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private Clock clock;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        Instant fixedInstant = now.atZone(ZoneId.systemDefault()).toInstant();
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
    }

    @Nested
    @DisplayName("내 쿠폰 목록 조회 시나리오 테스트")
    class Get_My_Coupons_Scenario {

        @Test
        @DisplayName("유효한 조회 요청이 주어지면 발급된 쿠폰 목록을 DTO로 변환하여 반환한다")
        void givenValidRequest_whenGetMyCoupons_thenReturnsMappedCouponResponses() {
            // given
            User user = User.forTest(1L, "user1@test.com", "pw", "유저1", UserRole.USER);
            MyCouponListRequest request = new MyCouponListRequest(null, IssuedCouponFilterType.ALL);
            Pageable pageable = Pageable.unpaged();

            Coupon coupon = Coupon.forTest(10L, "브랜드명", "할인쿠폰", "요약 설명", "상세 설명", 100, 10, now.minusDays(1), now.plusDays(1), CouponUseStatus.AVAILABLE);
            IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon);

            Page<IssuedCoupon> mockPage = new PageImpl<>(List.of(issuedCoupon));
            given(issuedCouponRepository.findAllByUser(any(), any(), any(), any())).willReturn(mockPage);

            // when
            PageResponse<MyCouponResponse> result = issuedCouponService.getIssuedCoupons(user, request, pageable);

            // then
            assertThat(result.totalElements()).isEqualTo(1);
            MyCouponResponse responseDto = result.content().get(0);
            assertThat(responseDto.title()).isEqualTo("할인쿠폰");
            assertThat(responseDto.status()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        }
    }

}
