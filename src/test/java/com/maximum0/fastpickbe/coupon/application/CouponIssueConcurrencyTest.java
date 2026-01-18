package com.maximum0.fastpickbe.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.User;
import com.maximum0.fastpickbe.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("쿠폰 발급 동시성 테스트")
class CouponIssueConcurrencyTest {

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

    private void setupClock(LocalDateTime dateTime) {
        Instant fixedInstant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
    }

    private void prepareTestData() {
        Coupon coupon = Coupon.create(
                "선착순 100명 쿠폰",
                threadCount,
                now.minusDays(1),
                now.plusDays(1)
        );
        couponId = couponRepository.save(coupon).getId();

        List<User> userList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            userList.add(User.create("user" + i + "@test.com", "pw", "테스터" + i));
        }
        users.addAll(userRepository.saveAll(userList));
    }

    @AfterEach
    void tearDown() {
        issuedCouponRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("100명의 유저가 동시에 쿠폰 발급을 요청하면, 발급된 수량이 정확히 100이 된다")
    void issue_maintainsCorrectQuantity_underConcurrentRequests() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();

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
        Coupon coupon = couponRepository.findActiveById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        long issuedCount = issuedCouponRepository.count();

        assertThat(issuedCount).isEqualTo(threadCount);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(threadCount);
        assertThat(exceptions).isEmpty();
    }

    @Test
    @DisplayName("한 명의 유저가 동시에 여러 번 발급을 요청해도 한 장만 발급된다")
    void issue_issuesOnlyOneCoupon_whenSameUserRequestsConcurrently() throws InterruptedException {
        // given
        int requestCount = 10;
        User user = users.get(0);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(requestCount);
        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();

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
        Coupon coupon = couponRepository.findActiveById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        long issuedCount = issuedCouponRepository.count();

        assertThat(issuedCount).isEqualTo(1);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
        assertThat(exceptions).hasSize(requestCount - 1);
    }

        

    @Test

    @DisplayName("쿠폰 수량보다 많은 유저가 동시에 발급을 요청해도, 수량만큼만 발급된다")

    void issue_issuesOnlyAvailableQuantity_whenMoreUsersRequestThanAvailable() throws InterruptedException {
        // given
        int availableQuantity = 50;
        int requestingUserCount = 70;
        List<User> newUsers = new ArrayList<>();
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < requestingUserCount; i++) {
            userList.add(User.create("over" + i + "@test.com", "pw", "테스터" + i));
        }
        newUsers.addAll(userRepository.saveAll(userList));

        Coupon overSubCoupon = Coupon.create("선착순 50명 쿠폰", availableQuantity, now.minusDays(1), now.plusDays(1));
        Long overSubCouponId = couponRepository.save(overSubCoupon).getId();

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestingUserCount);
        final List<Throwable> exceptions = new java.util.concurrent.CopyOnWriteArrayList<>();

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
        Coupon coupon = couponRepository.findActiveById(overSubCouponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        long issuedCount = issuedCouponRepository.countByCouponId(overSubCouponId);

        assertThat(issuedCount).isEqualTo(availableQuantity);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(availableQuantity);
        assertThat(exceptions).hasSize(requestingUserCount - availableQuantity);
    }

}
