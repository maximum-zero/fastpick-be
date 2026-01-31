package com.maximum0.fastpickbe.coupon.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CouponRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class})
@DisplayName("Coupon Repository 슬라이스 테스트")
class CouponRepositoryTest {

    @Autowired
    private CouponRepositoryImpl couponRepository;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 저장 및 단건 조회 시나리오 테스트")
    class Save_And_Find_Scenario {

        @Test
        @DisplayName("활성화된 쿠폰 식별자가 주어지면, 해당 쿠폰 정보를 반환한다")
        void givenActiveCoupon_whenFindActiveById_thenReturnsCoupon() {
            // given
            Coupon coupon = createCoupon("활성화 된 쿠폰", CouponUseStatus.AVAILABLE);
            Coupon saved = couponRepository.save(coupon);

            // when
            Optional<Coupon> found = couponRepository.findActiveById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("활성화 된 쿠폰");
        }

        @Test
        @DisplayName("중단된 쿠폰 식별자가 주어지면, 빈 값을 반환한다")
        void givenDisabledCoupon_whenFindActiveById_thenReturnsEmpty() {
            // given
            Coupon coupon = createCoupon("중단된 쿠폰", CouponUseStatus.DISABLED);
            Coupon saved = couponRepository.save(coupon);

            // when
            Optional<Coupon> found = couponRepository.findActiveById(saved.getId());

            // then
            assertThat(found).isEmpty();
        }

    }

    @Nested
    @DisplayName("쿠폰 목록 식별자 조회 시나리오 테스트")
    class Find_All_By_Ids_Scenario {

        @Test
        @DisplayName("유효한 식별자 목록이 주어지면, 모든 쿠폰 엔티티 목록을 반환한다")
        void givenValidIds_whenFindAllByIds_thenReturnsCouponList() {
            // given
            Coupon coupon1 = couponRepository.save(createCoupon("쿠폰1", CouponUseStatus.AVAILABLE));
            Coupon coupon2 = couponRepository.save(createCoupon("쿠폰2", CouponUseStatus.AVAILABLE));
            List<Long> targetIds = List.of(coupon1.getId(), coupon2.getId());

            // when
            List<Coupon> result = couponRepository.findAllByIds(targetIds);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Coupon::getId)
                    .containsExactlyInAnyOrder(coupon1.getId(), coupon2.getId());
        }

        @Test
        @DisplayName("빈 식별자 목록이 주어지면, 빈 리스트를 반환한다")
        void givenEmptyIds_whenFindAllByIds_thenReturnsEmptyList() {
            // when
            List<Coupon> result = couponRepository.findAllByIds(List.of());

            // then
            assertThat(result).isEmpty();
        }
    }

    // --- 테스트 메서드 ---

    private Coupon createCoupon(String title, CouponUseStatus status) {
        Coupon coupon = Coupon.create("브랜드", title, "요약", "상세",
                100, now.minusDays(1), now.plusDays(1));

        if (status == CouponUseStatus.DISABLED) {
            coupon.disable();
        }

        return coupon;
    }

}
