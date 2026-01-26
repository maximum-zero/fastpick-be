package com.maximum0.fastpickbe.coupon.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
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
@DisplayName("CouponAdmin Service 단위 테스트")
class CouponAdminServiceTest {

    @InjectMocks
    private CouponAdminService couponAdminService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponKeywordRepository couponKeywordRepository;

    @Mock
    private KeywordExtractor keywordExtractor;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 생성 테스트")
    class CreateCouponTest {

        @Test
        @DisplayName("쿠폰 정보가 주어지면 저장하고 키워드 인덱스를 생성한다")
        void createCoupon_SavesCouponAndKeywords_WhenCalledWithValidCoupon() {
            // given
            LocalDateTime now = LocalDateTime.now();
            Coupon coupon = Coupon.forTest(
                    1L,
                    "나이키",
                    "[특가] 에어포스",
                    "요약 설명",
                    "상세설명",
                    100,
                    0,
                    now,
                    now.plusDays(7)
            );

            given(couponRepository.save(any(Coupon.class))).willReturn(coupon);
            given(keywordExtractor.extract(any(), any())).willReturn(List.of("나이키", "특가", "에어포스"));

            // when
            Long resultId = couponAdminService.createCoupon(coupon);

            // then
            assertThat(resultId).isEqualTo(1L);

            verify(couponRepository, times(1)).save(coupon);
            verify(keywordExtractor).extract("나이키", "[특가] 에어포스");
            verify(couponKeywordRepository).saveAll(anyList());
        }
    }
}