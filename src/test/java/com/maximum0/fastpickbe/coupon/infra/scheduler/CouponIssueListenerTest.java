package com.maximum0.fastpickbe.coupon.infra.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.coupon.application.processor.CouponIssueBatchWorker;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Issue Listener 단위 테스트")
class CouponIssueListenerTest {

    @InjectMocks
    private CouponIssueListener couponIssueListener;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private CouponIssueBatchWorker couponIssueBatchWorker;

    @Mock
    private RSet<String> activeQueues;

    @Mock
    private RScoredSortedSet<String> waitingQueue;

    @Nested
    @DisplayName("스케줄러 실행(run) 테스트")
    class Run_Behavior {

        @Test
        @DisplayName("활성 큐가 비어있으면 배치를 실행하지 않는다")
        void CouponIssueListener_Run_DoesNothingWhenEmpty() {
            // given
            doReturn(activeQueues).when(redissonClient).getSet(anyString());
            given(activeQueues.isEmpty()).willReturn(true);

            // when
            couponIssueListener.run();

            // then
            // 활성 큐가 없으면 대기열 조회 및 배치 워커 호출이 발생하지 않아야 함
            verify(redissonClient, times(0)).getScoredSortedSet(anyString());
            verify(couponIssueBatchWorker, times(0)).issueBatch(anyLong(), any());
        }

        @Test
        @DisplayName("활성 큐에 데이터가 있으면 배치 처리 후 Redis에서 제거한다")
        void CouponIssueListener_Run_ProcessesAndRemovesFromRedis() {
            // given
            Long couponId = 1L;
            doReturn(activeQueues).when(redissonClient).getSet(anyString());
            given(activeQueues.isEmpty()).willReturn(false);

            // activeQueues.forEach 내부 콜백 로직 시뮬레이션
            doAnswer(invocation -> {
                Consumer<String> consumer = invocation.getArgument(0);
                consumer.accept(couponId.toString());
                return null;
            }).when(activeQueues).forEach(any());

            doReturn(waitingQueue).when(redissonClient).getScoredSortedSet(anyString());

            ScoredEntry<String> entry = new ScoredEntry<>(0.0, "user1");
            Collection<ScoredEntry<String>> batch = List.of(entry);

            // 첫 번째 호출에서 데이터를 반환하고, 두 번째 호출에서 빈 리스트를 반환하여 while 루프 종료 유도
            given(waitingQueue.entryRange(0, 99))
                    .willReturn(batch)
                    .willReturn(Collections.emptyList());

            // when
            couponIssueListener.run();

            // then
            // 워커에게 처리를 위임하고 처리가 완료된 항목은 Redis 큐에서 삭제되어야 함
            verify(couponIssueBatchWorker, times(1)).issueBatch(eq(couponId), eq(batch));
            verify(waitingQueue, times(1)).removeAll(anyList());
        }

        @Test
        @DisplayName("특정 큐 처리 중 예외가 발생해도 다른 큐 처리를 계속한다")
        void CouponIssueListener_Run_IsolatesExceptionsBetweenQueues() {
            // given
            String couponId1 = "1";
            String couponId2 = "2";

            doReturn(activeQueues).when(redissonClient).getSet(anyString());
            given(activeQueues.isEmpty()).willReturn(false);

            // 1번 쿠폰과 2번 쿠폰 대기열이 차례대로 처리되는 상황
            doAnswer(invocation -> {
                Consumer<String> consumer = invocation.getArgument(0);
                consumer.accept(couponId1);
                consumer.accept(couponId2);
                return null;
            }).when(activeQueues).forEach(any());

            doReturn(waitingQueue).when(redissonClient).getScoredSortedSet(anyString());

            // 1번 큐 처리 시 예외를 발생시키고, 2번 큐 처리 시 정상 데이터를 반환하도록 설정
            given(waitingQueue.entryRange(0, 99))
                    .willThrow(new RuntimeException("Redis Error"))
                    .willReturn(List.of(new ScoredEntry<>(0.0, "user2")))
                    .willReturn(Collections.emptyList());

            // when
            couponIssueListener.run();

            // then
            // 1번 큐에서 예외가 발생했더라도 2번 쿠폰(2L)에 대한 워커 호출은 정상적으로 수행되어야 함 (격리성 검증)
            verify(couponIssueBatchWorker, times(1)).issueBatch(eq(2L), any());
        }
    }

}
