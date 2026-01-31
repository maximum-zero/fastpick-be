package com.maximum0.fastpickbe.coupon.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponFilterType;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({IssuedCouponRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class})
@DisplayName("IssuedCoupon Repository 슬라이스 테스트")
class IssuedCouponRepositoryTest {

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("중복 발급 체크 시나리오 테스트")
    class Duplication_Check_Scenario {

        private User user;
        private Coupon coupon;

        @BeforeEach
        void setUp() {
            user = entityManager.persist(User.create("test@test.com", "pw", "테스터"));
            coupon = entityManager.persist(Coupon.create("브랜드", "테스트쿠폰", "요약", "상세", 100, now, now.plusDays(1)));

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("유저가 이미 쿠폰을 발급받았다면 true를 반환한다")
        void givenIssuedRecord_whenExistsByUserAndCoupon_thenReturnsTrue() {
            // given
            User targetUser = entityManager.find(User.class, user.getId());
            Coupon targetCoupon = entityManager.find(Coupon.class, coupon.getId());
            issuedCouponRepository.save(IssuedCoupon.create(targetUser, targetCoupon));
            entityManager.flush();

            // when
            boolean exists = issuedCouponRepository.existsByUserAndCoupon(targetUser, targetCoupon);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("발급 이력이 없다면 false를 반환한다")
        void givenNoRecord_whenExistsByUserAndCoupon_thenReturnsFalse() {
            // given
            User targetUser = entityManager.find(User.class, user.getId());
            Coupon targetCoupon = entityManager.find(Coupon.class, coupon.getId());

            // when
            boolean exists = issuedCouponRepository.existsByUserAndCoupon(targetUser, targetCoupon);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("내 쿠폰 목록 조회 시나리오 테스트")
    class Find_My_Coupons_Scenario {
        private User user1;
        private Coupon couponA;

        @BeforeEach
        void setUp() {
            user1 = entityManager.persist(User.create("user1@test.com", "pw", "유저1"));
            User user2 = entityManager.persist(User.create("user2@test.com", "pw", "유저2"));

            couponA = entityManager.persist(Coupon.create("브랜드", "할인쿠폰A", "요약", "상세", 100, now.minusDays(10), now.plusDays(10)));
            Coupon couponB = entityManager.persist(Coupon.create("브랜드", "할인쿠폰B", "요약", "상세", 100, now.minusDays(10), now.plusDays(10)));
            Coupon expiredCoupon = entityManager.persist(Coupon.create("브랜드", "만료된쿠폰", "요약", "상세", 100, now.minusDays(20), now.minusDays(10)));

            // 발급 이력 생성 및 영속화
            IssuedCoupon issued1 = IssuedCoupon.create(user1, couponA);
            IssuedCoupon issued2 = IssuedCoupon.create(user1, couponB);
            issued2.use(now); // 사용 완료 처리
            IssuedCoupon issued3 = IssuedCoupon.create(user1, expiredCoupon);

            entityManager.persist(issued1);
            entityManager.persist(issued2);
            entityManager.persist(issued3);
            entityManager.persist(IssuedCoupon.create(user2, couponA));

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("전체 필터로 조회하면, 사용자가 보유한 모든 쿠폰 목록이 반환된다")
        void givenFilterAll_whenFindAllByUser_thenReturnsAllCoupons() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, IssuedCouponFilterType.ALL);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("쿠폰 제목 키워드로 조회하면, 해당 제목을 포함한 쿠폰만 반환된다")
        void givenTitleKeyword_whenFindAllByUser_thenReturnsFilteredByTitle() {
            // given
            MyCouponListRequest request = new MyCouponListRequest("쿠폰A", IssuedCouponFilterType.ALL);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCoupon().getTitle()).contains("쿠폰A");
        }

        @Test
        @DisplayName("사용 가능 필터로 조회하면, 미사용이며 기간 내인 쿠폰만 반환된다")
        void givenFilterAvailable_whenFindAllByUser_thenReturnsAvailableCoupons() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, IssuedCouponFilterType.AVAILABLE);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCoupon().getId()).isEqualTo(couponA.getId());
        }

        @Test
        @DisplayName("만료 필터로 조회하면, 사용 완료되었거나 기간이 지난 쿠폰이 반환된다")
        void givenFilterExpired_whenFindAllByUser_thenReturnsExpiredOrUsedCoupons() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, IssuedCouponFilterType.EXPIRED);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("사용 완료 필터로 조회하면, 이미 사용한 쿠폰 목록만 반환된다")
        void givenFilterUsed_whenFindAllByUser_thenReturnsUsedCoupons() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, IssuedCouponFilterType.USED);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).isUsed()).isTrue();
        }
    }

}
