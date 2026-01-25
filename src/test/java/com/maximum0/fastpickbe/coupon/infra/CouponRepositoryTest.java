package com.maximum0.fastpickbe.coupon.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
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
            Coupon coupon = Coupon.create("브랜드", title, "요약 설명", "상세 설명", 100, now.minusDays(1), now.plusDays(1));
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
            Coupon coupon = Coupon.create("브랜드", "중단된 쿠폰", "요약 설명", "상세 설명", 100, now.minusDays(1), now.plusDays(1));
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
            Coupon coupon = Coupon.create("브랜드", "락 테스트 쿠폰", "요약 설명", "상세 설명", 100, now.minusDays(1), now.plusDays(1));
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
    @DisplayName("쿠폰 목록 식별자 조회 테스트")
    class FindAllByIdsTest {

        @Test
        @DisplayName("식별자 목록이 주어지면 해당하는 모든 쿠폰 엔티티를 반환한다")
        void findAllByIds_ReturnsCoupons_WhenIdsAreProvided() {
            // given
            Coupon coupon1 = couponRepository.save(Coupon.create("나이키", "쿠폰1", "요약 설명", "상세 설명", 100, now.minusDays(1), now.plusDays(1)));
            Coupon coupon2 = couponRepository.save(Coupon.create("아디다스", "쿠폰2", "요약 설명", "상세 설명", 100, now.minusDays(1), now.plusDays(1)));
            List<Long> targetIds = List.of(coupon1.getId(), coupon2.getId());

            // when
            List<Coupon> result = couponRepository.findAllByIds(targetIds);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Coupon::getId)
                    .containsExactlyInAnyOrder(coupon1.getId(), coupon2.getId());
        }

        @Test
        @DisplayName("빈 식별자 목록이 주어지면 빈 리스트를 반환한다")
        void findAllByIds_ReturnsEmptyList_WhenIdsAreEmpty() {
            // when
            List<Coupon> result = couponRepository.findAllByIds(List.of());

            // then
            assertThat(result).isEmpty();
        }
    }

}