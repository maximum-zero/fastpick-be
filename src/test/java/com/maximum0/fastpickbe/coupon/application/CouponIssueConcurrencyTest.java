package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.base.BaseIntegrationTest;
import com.maximum0.fastpickbe.coupon.application.service.CouponIssueService;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DisplayName("Coupon Issue 동시성 통합 테스트")
class CouponIssueConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private CouponIssueService couponIssueService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @MockitoBean
    private Clock clock;

    private Long couponId;
    private final List<User> users = new ArrayList<>();
    private final int threadCount = 100;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 1, 0);

    @BeforeEach
    void setUp() {
        users.clear();
        setupClock(now);
        prepareTestData();
    }

    @AfterEach
    void tearDown() {
        issuedCouponRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("쿠폰 발급 동시 요청 시나리오 테스트")
    class Coupon_Issue_Concurrency_Scenario {

        @Test
        @DisplayName("100명의 유저가 동시에 발급을 요청하면 정확히 100장의 쿠폰이 발급되어야 한다")
        void givenMultipleUsers_whenConcurrentIssue_thenMaintainsCorrectQuantity() throws InterruptedException {
            // given
            ExecutorService executorService = Executors.newFixedThreadPool(32);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Throwable> exceptions = new CopyOnWriteArrayList<>();

            // when
            for (User user : users) {
                executorService.submit(() -> {
                    try {
                        couponIssueService.issue(couponId, user);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then
            Coupon coupon = getCoupon(couponId);
            long issuedCount = issuedCouponRepository.count();

            assertThat(issuedCount).isEqualTo(threadCount);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(threadCount);
            assertThat(exceptions).isEmpty();
        }

        @Test
        @DisplayName("동일 유저가 동시에 여러 번 발급을 시도해도 단 한 장의 쿠폰만 발급되어야 한다")
        void givenSameUser_whenConcurrentIssue_thenIssuesOnlyOneCoupon() throws InterruptedException {
            // given
            int requestCount = 10;
            User user = users.get(0);
            ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
            CountDownLatch latch = new CountDownLatch(requestCount);
            List<Throwable> exceptions = new CopyOnWriteArrayList<>();

            // when
            for (int i = 0; i < requestCount; i++) {
                executorService.submit(() -> {
                    try {
                        couponIssueService.issue(couponId, user);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then
            Coupon coupon = getCoupon(couponId);
            long issuedCount = issuedCouponRepository.count();

            assertThat(issuedCount).isEqualTo(1);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
            assertThat(exceptions).hasSize(requestCount - 1);
        }

        @Test
        @DisplayName("재고보다 많은 유저가 동시에 요청하면 재고 수량만큼만 발급되어야 한다")
        void givenExceedingUsers_whenConcurrentIssue_thenIssuesOnlyAvailableQuantity() throws InterruptedException {
            // given
            int availableQuantity = 50;
            int requestingUserCount = 70;
            List<User> newUsers = createAndSaveUsers(requestingUserCount, "over");
            Long overSubCouponId = createAndSaveCoupon(availableQuantity);

            ExecutorService executorService = Executors.newFixedThreadPool(32);
            CountDownLatch latch = new CountDownLatch(requestingUserCount);
            List<Throwable> exceptions = new CopyOnWriteArrayList<>();

            // when
            for (User user : newUsers) {
                executorService.submit(() -> {
                    try {
                        couponIssueService.issue(overSubCouponId, user);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            // then
            Coupon coupon = getCoupon(overSubCouponId);
            long issuedCount = issuedCouponRepository.countByCouponId(overSubCouponId);

            assertThat(issuedCount).isEqualTo(availableQuantity);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(availableQuantity);
            assertThat(exceptions).hasSize(requestingUserCount - availableQuantity);
        }
    }

    // --- 테스트 헬퍼 메서드 ---

    private void setupClock(LocalDateTime dateTime) {
        Instant fixedInstant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
    }

    private void prepareTestData() {
        couponId = createAndSaveCoupon(threadCount);
        users.addAll(createAndSaveUsers(threadCount, "user"));
    }

    private Long createAndSaveCoupon(int quantity) {
        Coupon coupon = Coupon.create("29CM", "선착순 쿠폰", "요약", "상세", quantity, now.minusDays(1), now.plusDays(1));
        return couponRepository.save(coupon).getId();
    }

    private List<User> createAndSaveUsers(int count, String prefix) {
        List<User> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(User.create(prefix + i + "@test.com", "pw", "테스터" + i));
        }
        return userRepository.saveAll(list);
    }

    private Coupon getCoupon(Long id) {
        return couponRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
    }

}
