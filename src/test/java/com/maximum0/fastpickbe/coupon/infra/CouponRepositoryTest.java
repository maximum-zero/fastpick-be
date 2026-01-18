package com.maximum0.fastpickbe.coupon.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CouponRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class})
@DisplayName("Coupon Repository 단위 테스트")
class CouponRepositoryTest {

    @Autowired
    private CouponRepositoryImpl couponRepository;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 1, 0);

    @Nested
    @DisplayName("쿠폰 저장 및 단건 조회 테스트")
    class SaveAndFindTest {

        @Test
        @DisplayName("활성화된 쿠폰은 ID로 조회할 수 있다")
        void saveAndFindActiveById_returnsCoupon_whenCouponIsActive() {
            // given
            String title = "활성화 된 쿠폰";
            Coupon coupon = Coupon.create(title, 100, now.minusDays(1), now.plusDays(1));
            Coupon saved = couponRepository.save(coupon);

            // when
            Optional<Coupon> found = couponRepository.findActiveById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo(title);
        }

        @Test
        @DisplayName("중단된(DISABLED) 쿠폰은 ID로 조회해도 빈 Optional을 반환한다")
        void findActiveById_returnsEmpty_whenCouponIsDisabled() {
            // given
            Coupon coupon = Coupon.create("중단된 쿠폰", 100, now.minusDays(1), now.plusDays(1));
            coupon.disable();
            Coupon saved = couponRepository.save(coupon);

            // when
            Optional<Coupon> found = couponRepository.findActiveById(saved.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("비관적 락을 적용하여 쿠폰을 조회할 수 있다")
        void findByIdWithLock_returnsCoupon_withPessimisticLock() {
            // given
            Coupon coupon = Coupon.create("락 테스트 쿠폰", 100, now.minusDays(1), now.plusDays(1));
            Coupon saved = couponRepository.save(coupon);

            // when
            Optional<Coupon> found = couponRepository.findByIdWithLock(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getTitle()).isEqualTo("락 테스트 쿠폰");
            // 로그상에 'for no key update'가 포함되는지 확인
        }
    }

    @Nested
    @DisplayName("쿠폰 목록 필터링 테스트")
    class FindAllByFilterTest {

        @Test
        @DisplayName("발급 중(ISSUING) 필터 적용 시 조건에 맞는 쿠폰만 반환한다")
        void findAll_returnsOnlyIssuingCoupons_whenFilterTypeIsIssuing() {
            // given
            couponRepository.save(Coupon.create("발급 중", 100, now.minusDays(1), now.plusDays(1)));
            couponRepository.save(Coupon.create("발급 대기", 100, now.plusDays(1), now.plusDays(2)));
            couponRepository.save(Coupon.create("만료", 100, now.minusDays(2), now.minusDays(1)));

            CouponListRequest request = new CouponListRequest(null, CouponFilterType.ISSUING);

            // when
            Page<CouponSummaryResponse> result = couponRepository.findAll(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("발급 중");
        }

        @Test
        @DisplayName("제목 검색 조건이 있으면 해당 키워드가 포함된 쿠폰만 반환한다")
        void findAll_returnsFilteredCoupons_whenTitleKeywordProvided() {
            // given
            couponRepository.save(Coupon.create("여름 할인 쿠폰", 100, now.minusDays(1), now.plusDays(1)));
            couponRepository.save(Coupon.create("겨울 할인 쿠폰", 100, now.minusDays(1), now.plusDays(1)));

            CouponListRequest request = new CouponListRequest("여름", CouponFilterType.ALL);

            // when
            Page<CouponSummaryResponse> result = couponRepository.findAll(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).contains("여름");
        }
    }
}