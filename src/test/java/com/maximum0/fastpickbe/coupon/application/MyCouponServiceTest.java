package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponUseStatus;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatus;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatusFilter;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.User;
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
@DisplayName("MyCouponService 단위 테스트")
class MyCouponServiceTest {

    @InjectMocks
    private MyCouponService myCouponService;

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
    @DisplayName("내 쿠폰 목록 조회 테스트")
    class GetMyCouponsTest {

        @Test
        @DisplayName("조회된 쿠폰 엔티티 목록을 DTO로 변환하여 반환한다")
        void getMyCoupons_mapsEntitiesToDto_andReturns() {
            // given
            User user = User.forTest(1L, "user1@test.com", "pw", "유저1");
            MyCouponListRequest request = new MyCouponListRequest(null, MyCouponStatusFilter.ALL);
            Pageable pageable = Pageable.unpaged();

            Coupon coupon = Coupon.forTest(10L, "브랜드명", "할인쿠폰", "요약 설명", "상세 설명", 100, 10, now.minusDays(1), now.plusDays(1), CouponUseStatus.AVAILABLE);
            IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon);
            
            Page<IssuedCoupon> mockPage = new PageImpl<>(List.of(issuedCoupon));
            given(issuedCouponRepository.findAllByUser(any(), any(), any(), any())).willReturn(mockPage);

            // when
            Page<MyCouponResponse> result = myCouponService.getMyCoupons(user, request, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            MyCouponResponse responseDto = result.getContent().get(0);
            assertThat(responseDto.title()).isEqualTo("할인쿠폰");
            assertThat(responseDto.status()).isEqualTo(MyCouponStatus.AVAILABLE);
        }
    }
}
