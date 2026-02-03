package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.base.BaseIntegrationTest;
import com.maximum0.fastpickbe.coupon.application.service.CouponIssueService;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.infra.scheduler.CouponIssueListener;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CouponIssueListener couponIssueListener;

    @MockitoBean
    private Clock clock;

    private Long couponId;
    private final List<User> users = new ArrayList<>();
    private final int threadCount = 100;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        redissonClient.getKeys().flushall();

        issuedCouponRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();

        users.clear();
        setupClock(now);

        prepareTestData();
    }

    @Nested
    @DisplayName("비동기 선착순 발급 시나리오 테스트")
    class Async_Coupon_Issue_Scenario {

        @Test
        @DisplayName("100명의 사용자가 동시에 발급을 요청하면 백그라운드 스케줄러를 통해 정확히 100건이 발급되어야 한다")
        void givenConcurrentRequests_whenIssueCoupon_thenMaintainsCorrectQuantity() throws InterruptedException {
            // given
            ExecutorService executorService = Executors.newFixedThreadPool(32);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // when
            for (User user : users) {
                executorService.submit(() -> {
                    try {
                        couponIssueService.issue(couponId, user.getId());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            couponIssueListener.run();
            executorService.shutdown();

            // then
            verifyIssuedCount(couponId, threadCount);
        }

        @Test
        @DisplayName("동일 사용자가 동시에 여러 번 발급을 시도해도 최종적으로 1건만 처리되어야 한다")
        void givenConcurrentSameUserRequests_whenIssueCoupon_thenIssuesOnlyOne() throws InterruptedException {
            // given
            int requestCount = 10;
            User user = users.get(0);
            ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
            CountDownLatch latch = new CountDownLatch(requestCount);

            // when
            for (int i = 0; i < requestCount; i++) {
                executorService.submit(() -> {
                    try {
                        couponIssueService.issue(couponId, user.getId());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            couponIssueListener.run();
            executorService.shutdown();

            // then
            verifyIssuedCount(couponId, 1);
        }

        @Test
        @DisplayName("쿠폰 잔여 재고보다 많은 수의 요청이 동시에 들어오면 재고만큼만 발급되어야 한다")
        void givenRequestsExceedingQuantity_whenIssueCoupon_thenIssuesOnlyAvailable() throws InterruptedException {
            // given
            int availableQuantity = 50;
            int requestingCount = 70;
            Long limitedCouponId = createAndSaveCoupon(availableQuantity);
            List<User> extraUsers = createAndSaveUsers(requestingCount, "extra");

            ExecutorService executorService = Executors.newFixedThreadPool(32);
            CountDownLatch latch = new CountDownLatch(requestingCount);

            // when
            for (User user : extraUsers) {
                executorService.submit(() -> {
                    try {
                        couponIssueService.issue(limitedCouponId, user.getId());
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            couponIssueListener.run();
            executorService.shutdown();

            // then
            verifyIssuedCount(limitedCouponId, availableQuantity);
        }
    }

    // --- 테스트 메서드 ---

    /**
     * 비동기 스케줄러의 처리가 완료될 때까지 DB 상태를 관측하여 정합성을 검증한다.
     *
     * @param targetCouponId 검증 대상 쿠폰 ID
     * @param expected       기대 발급 수량
     */
    private void verifyIssuedCount(Long targetCouponId, int expected) {
        org.awaitility.Awaitility.await()
                .atMost(10, java.util.concurrent.TimeUnit.SECONDS)
                .pollInterval(1, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long dbCount = issuedCouponRepository.countByCouponId(targetCouponId);
                    Coupon coupon = couponRepository.findActiveById(targetCouponId)
                            .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

                    assertThat(dbCount).isEqualTo(expected);
                    assertThat(coupon.getIssuedQuantity()).isEqualTo(expected);
                });
    }

    /**
     * 테스트에 필요한 Mock Clock 환경을 설정한다.
     */
    private void setupClock(LocalDateTime dateTime) {
        Instant fixedInstant = dateTime.atZone(KST).toInstant();
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(KST);
        given(clock.millis()).willReturn(fixedInstant.toEpochMilli());
    }

    /**
     * 테스트 시작 전 기본 데이터를 준비한다.
     */
    private void prepareTestData() {
        couponId = createAndSaveCoupon(threadCount);
        users.addAll(createAndSaveUsers(threadCount, "user"));
    }

    /**
     * 지정된 재고의 쿠폰을 생성한다.
     */
    private Long createAndSaveCoupon(int quantity) {
        Coupon coupon = Coupon.create("29CM", "할인 쿠폰", "요약", "상세", quantity, now.minusDays(1), now.plusDays(1));
        return couponRepository.save(coupon).getId();
    }

    /**
     * 사용자 목록을 벌크로 생성한다.
     */
    private List<User> createAndSaveUsers(int count, String prefix) {
        List<User> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(User.create(prefix + i + "@29cm.com", "pw", "사용자" + i));
        }
        return userRepository.saveAll(list);
    }
}
