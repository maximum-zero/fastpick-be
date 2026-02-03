package com.maximum0.fastpickbe.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Issue Service 단위 테스트")
class CouponIssueServiceTest {

    @InjectMocks
    private CouponIssueService couponIssueService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RedissonClient redissonClient;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private Clock clock;

    @Mock
    private RScoredSortedSet<String> mockSSet;

    @Mock
    private RSet<String> mockUserSet;

    @Mock
    private RAtomicLong mockStock;

    @Mock
    private RBucket<String> mockBucket;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
    private User testUser;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = now.atZone(KST).toInstant();

        given(clock.getZone()).willReturn(KST);
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.millis()).willReturn(fixedInstant.toEpochMilli());

        // Warm-up 시나리오를 위한 기본 쿠폰 데이터 모킹
        Coupon mockCoupon = Coupon.create("29CM", "선착순 쿠폰", "요약", "상세", 100, now.minusDays(1), now.plusDays(1));
        given(couponRepository.findActiveById(anyLong())).willReturn(Optional.of(mockCoupon));

        testUser = User.forTest(1L, "test@test.com", "password", "user", UserRole.USER);
    }

    @Nested
    @DisplayName("비동기 쿠폰 발급 요청 테스트")
    class Issue_Coupon_Asynchronous {

        @Test
        @DisplayName("발급 요청 시 Redis 대기열에 유저 ID와 타임스탬프가 정상 적재된다")
        void givenValidRequest_whenIssue_thenAddsToRedisQueue() {
            // given
            Long couponId = 1L;

            // Redis 의존성 객체 연결 및 상태 설정
            doReturn(mockStock).when(redissonClient).getAtomicLong(anyString());
            doReturn(mockUserSet).when(redissonClient).getSet(anyString());
            doReturn(mockSSet).when(redissonClient).getScoredSortedSet(anyString());

            given(mockStock.isExists()).willReturn(true);                           // 재고 Warm-up 완료 상태
            given(mockUserSet.add(anyString())).willReturn(true);                   // 중복 발급 아님
            given(mockStock.decrementAndGet()).willReturn(99L);                     // 재고 차감 성공
            doReturn(true).when(mockSSet).add(anyDouble(), anyString());   // 대기열 진입 성공

            // when
            couponIssueService.issue(couponId, testUser.getId());

            // then
            verify(mockSSet, times(1)).add(
                    eq((double) clock.millis()),
                    eq(testUser.getId().toString())
            );
        }

        @Test
        @DisplayName("이미 대기열에 존재하는 유저라면 중복 발급 예외를 던진다")
        void givenDuplicateUser_whenIssue_thenThrowsException() {
            // given
            Long couponId = 1L;

            doReturn(mockStock).when(redissonClient).getAtomicLong(anyString());
            doReturn(mockUserSet).when(redissonClient).getSet(anyString());

            given(mockStock.isExists()).willReturn(true);
            given(mockUserSet.add(testUser.getId().toString())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(couponId, testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ISSUED_COUPON);

            // 중복 유저라면 재고 차감(decrement) 시도조차 하지 않아야 함
            verify(mockStock, times(0)).decrementAndGet();
        }

        @Test
        @DisplayName("재고가 소진된 상태에서 요청하면 예외를 던지고 재고를 복구한다")
        void givenExhaustedStock_whenIssue_thenThrowsExceptionAndRestoresStock() {
            // given
            doReturn(mockStock).when(redissonClient).getAtomicLong(anyString());
            doReturn(mockUserSet).when(redissonClient).getSet(anyString());

            given(mockStock.isExists()).willReturn(true);
            given(mockUserSet.add(anyString())).willReturn(true);
            given(mockStock.decrementAndGet()).willReturn(-1L); // 차감 후 재고가 음수인 상황

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(1L, testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_EXHAUSTED);

            // 차감했던 재고를 다시 원상복구(Rollback) 하는지 확인
            verify(mockStock, times(1)).incrementAndGet();
        }

        @Test
        @DisplayName("대기열 적재 중 예외 발생 시 차감된 재고와 중복 체크 데이터를 롤백한다")
        void givenQueueError_whenIssue_thenRollbacksAllState() {
            // given
            doReturn(mockStock).when(redissonClient).getAtomicLong(anyString());
            doReturn(mockUserSet).when(redissonClient).getSet(anyString());
            doReturn(mockSSet).when(redissonClient).getScoredSortedSet(anyString());

            given(mockStock.isExists()).willReturn(true);
            given(mockUserSet.add(anyString())).willReturn(true);
            given(mockStock.decrementAndGet()).willReturn(10L);

            // Redis 통신 장애 등으로 인한 예외 발생 시나리오
            given(mockSSet.add(anyDouble(), anyString())).willThrow(new RuntimeException("Redis Error"));

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(1L, testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_LOCKING_ERROR);

            // 원자성 보장을 위한 재고 및 유저 롤백 확인
            verify(mockUserSet, times(1)).add(testUser.getId().toString());
            verify(mockUserSet, times(1)).remove(testUser.getId().toString());
            verify(mockStock, times(1)).incrementAndGet();
        }

        @Test
        @DisplayName("발급 종료 시간이 지난 쿠폰에 대해 요청하면 예외를 던진다")
        void givenExpiredCoupon_whenIssue_thenThrowsException() {
            // given
            Long couponId = 1L;
            String expiredTime = String.valueOf(clock.millis() - 1000); // Stubbing 충돌 방지를 위해 값을 미리 계산

            doReturn(mockStock).when(redissonClient).getAtomicLong(anyString());
            given(mockStock.isExists()).willReturn(true);

            doReturn(mockBucket).when(redissonClient).getBucket(anyString());
            given(mockBucket.get()).willReturn(expiredTime);    // 캐싱된 종료 시각이 현재보다 과거

            // when & then
            assertThatThrownBy(() -> couponIssueService.issue(couponId, testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_AVAILABLE_PERIOD);
        }

    }

}
