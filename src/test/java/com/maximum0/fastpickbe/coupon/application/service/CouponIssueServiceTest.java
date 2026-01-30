package com.maximum0.fastpickbe.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.application.facade.CouponIssueExecutor;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Issue Service 단위 테스트")
class CouponIssueServiceTest {
    @InjectMocks
    private CouponIssueService couponIssueService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private CouponIssueExecutor couponIssueExecutor;

    @Mock
    private Clock clock;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final Instant fixedInstant = now.atZone(ZoneId.systemDefault()).toInstant();
    private User testUser;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = now.atZone(ZoneId.systemDefault()).toInstant();
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.systemDefault());

        testUser = User.forTest(1L, "test@test.com", "password", "user");
    }

    @Nested
    @DisplayName("쿠폰 발급 테스트")
    class IssueCouponTest {
        @Test
        @DisplayName("락 획득에 성공하면 Executor를 통해 발급을 수행한다")
        void issue_LockAcquired_Success() throws InterruptedException {
            // given
            Long couponId = 1L;
            RLock mockLock = mock(RLock.class);

            given(redissonClient.getLock(anyString())).willReturn(mockLock);
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
            given(mockLock.isHeldByCurrentThread()).willReturn(true);

            given(couponIssueExecutor.executeIssue(anyLong(), any(User.class), any()))
                    .willReturn(100L);

            // when
            Long issuedId = couponIssueService.issue(couponId, testUser);

            // then
            assertThat(issuedId).isEqualTo(100L);
            verify(couponIssueExecutor).executeIssue(anyLong(), any(User.class), any());
            verify(mockLock).unlock();
        }

        @Test
        @DisplayName("락 획득에 실패하면 CONCURRENCY_BUSY 예외를 던진다")
        void issue_LockAcquisitionFails_ThrowsException() throws InterruptedException {
            // given
            Long couponId = 1L;
            RLock mockLock = mock(RLock.class);

            given(redissonClient.getLock(anyString())).willReturn(mockLock);
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(couponId, testUser))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONCURRENCY_BUSY);

            verifyNoInteractions(couponIssueExecutor);
        }
    }
}