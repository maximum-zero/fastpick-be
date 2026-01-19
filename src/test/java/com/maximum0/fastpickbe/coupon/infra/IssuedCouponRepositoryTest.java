package com.maximum0.fastpickbe.coupon.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatusFilter;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.user.domain.User;
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
@DisplayName("IssuedCoupon Repository 단위 테스트")
class IssuedCouponRepositoryTest {

    @Autowired
    private IssuedCouponRepositoryImpl issuedCouponRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("중복 발급 체크 테스트")
    class DuplicationCheckTest {

        private User user;
        private Coupon coupon;

        @BeforeEach
        void setUp() {
            user = User.create("test@test.com", "encodedPassword", "테스터");
            entityManager.persist(user);

            coupon = Coupon.create("테스트쿠폰", 100, now, now.plusDays(1));
            entityManager.persist(coupon);

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("유저가 이미 쿠폰을 발급받았다면 true를 반환한다")
        void existsByUserAndCoupon_returnsTrue_whenRecordExists() {
            // given
            User user = entityManager.find(User.class, this.user.getId());
            Coupon coupon = entityManager.find(Coupon.class, this.coupon.getId());
            IssuedCoupon issuedCoupon = IssuedCoupon.create(user, coupon);
            issuedCouponRepository.save(issuedCoupon);
            entityManager.flush();

            // when
            boolean exists = issuedCouponRepository.existsByUserAndCoupon(user, coupon);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("발급 이력이 없다면 false를 반환한다")
        void existsByUserAndCoupon_returnsFalse_whenRecordNotExists() {
            // given
            User user = entityManager.find(User.class, this.user.getId());
            Coupon coupon = entityManager.find(Coupon.class, this.coupon.getId());

            // when
            boolean exists = issuedCouponRepository.existsByUserAndCoupon(user, coupon);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("내 쿠폰 목록 조회 테스트")
    class FindMyCouponsTest {
        private User user1;
        private User user2;
        private Coupon couponA;

        @BeforeEach
        void setUp() {
            user1 = entityManager.persist(User.create("user1@test.com", "pw", "유저1"));
            user2 = entityManager.persist(User.create("user2@test.com", "pw", "유저2"));

            couponA = entityManager.persist(Coupon.create("할인쿠폰A", 100, now.minusDays(10), now.plusDays(10)));
            Coupon couponB = entityManager.persist(Coupon.create("할인쿠폰B", 100, now.minusDays(10), now.plusDays(10)));
            Coupon expiredCoupon = entityManager.persist(Coupon.create("만료된쿠폰", 100, now.minusDays(20), now.minusDays(10)));

            IssuedCoupon issued1 = IssuedCoupon.create(user1, couponA);
            IssuedCoupon issued2 = IssuedCoupon.create(user1, couponB);
            issued2.use(now);
            IssuedCoupon issued3 = IssuedCoupon.create(user1, expiredCoupon);
            entityManager.persist(issued1);
            entityManager.persist(issued2);
            entityManager.persist(issued3);

            entityManager.persist(IssuedCoupon.create(user2, couponA));

            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("user1로 조회하면, user1에게 발급된 모든 쿠폰 3개가 조회된다.")
        void findMyCoupons_returnsAllCoupons_forUser1() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, MyCouponStatusFilter.ALL);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("쿠폰 제목으로 검색하면, 해당 쿠폰만 조회된다.")
        void findMyCoupons_returnsFilteredCoupons_byTitle() {
            // given
            MyCouponListRequest request = new MyCouponListRequest("쿠폰A", MyCouponStatusFilter.ALL);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCoupon().getTitle()).contains("쿠폰A");
        }

        @Test
        @DisplayName("'사용 가능' 상태로 필터링하면, 사용하지 않았고 만료되지 않은 쿠폰 1개가 조회된다.")
        void findMyCoupons_returnsAvailableCoupons_whenFilteredByAvailable() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, MyCouponStatusFilter.AVAILABLE);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCoupon().getId()).isEqualTo(couponA.getId());
        }

        @Test
        @DisplayName("'만료' 상태로 필터링하면, 사용했거나 기간이 지난 쿠폰 2개가 조회된다.")
        void findMyCoupons_returnsExpiredCoupons_whenFilteredByExpired() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, MyCouponStatusFilter.EXPIRED);
            PageRequest pageable = PageRequest.of(0, 10);
            User userToFind = entityManager.find(User.class, user1.getId());

            // when
            Page<IssuedCoupon> result = issuedCouponRepository.findAllByUser(userToFind, request, pageable, now);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("'사용 완료' 상태로 필터링하면, 사용한 쿠폰 1개가 조회된다.")
        void findMyCoupons_returnsUsedCoupons_whenFilteredByUsed() {
            // given
            MyCouponListRequest request = new MyCouponListRequest(null, MyCouponStatusFilter.USED);
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