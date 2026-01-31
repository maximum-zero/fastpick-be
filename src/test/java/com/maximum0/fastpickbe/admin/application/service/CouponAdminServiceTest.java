package com.maximum0.fastpickbe.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.admin.coupon.application.service.CouponAdminService;
import com.maximum0.fastpickbe.coupon.application.service.CouponKeywordManager;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Admin Service 단위 테스트")
class CouponAdminServiceTest {

    @InjectMocks
    private CouponAdminService couponAdminService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponKeywordRepository couponKeywordRepository;

    @Mock
    private CouponKeywordManager couponKeywordManager;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 생성 시나리오 테스트")
    class Create_Coupon_Scenario {

        @Test
        @DisplayName("올바른 쿠폰 정보가 주어지면 쿠폰을 저장하고 키워드 인덱스를 정상적으로 생성한다")
        void givenValidCoupon_whenCreateCoupon_thenReturnsCouponId() {
            // given
            Coupon coupon = createTestCoupon();
            List<String> keywords = List.of("나이키", "특가", "에어포스");

            given(couponRepository.save(any(Coupon.class))).willReturn(coupon);
            given(couponKeywordManager.extract(coupon.getBrand(), coupon.getTitle())).willReturn(keywords);

            // when
            Long resultId = couponAdminService.createCoupon(coupon);

            // then
            assertThat(resultId).isEqualTo(1L);

            verify(couponRepository, times(1)).save(any(Coupon.class));
            verify(couponKeywordManager, times(1)).extract(coupon.getBrand(), coupon.getTitle());
            verify(couponKeywordRepository, times(1)).saveAll(anyList());
        }
    }

    // --- 테스트 메서드 ---

    private Coupon createTestCoupon() {
        return Coupon.forTest(
                1L, "나이키", "[특가] 에어포스", "요약", "상세",
                100, 0, now, now.plusDays(7), CouponUseStatus.AVAILABLE
        );
    }

}
