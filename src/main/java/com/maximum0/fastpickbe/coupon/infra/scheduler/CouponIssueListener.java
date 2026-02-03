package com.maximum0.fastpickbe.coupon.infra.scheduler;

import com.maximum0.fastpickbe.coupon.application.processor.CouponIssueBatchWorker;
import com.maximum0.fastpickbe.coupon.infra.redis.CouponRedisSchema;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 대기열에 적재된 쿠폰 발급 요청을 비동기적으로 처리하여 DB에 저장하는 리스너.
 *
 * - 스케줄러를 통해 주기적으로 활성 큐를 감시한다.
 * - Redis Sorted Set에서 요청 순서(Score)대로 유저를 추출하여 배치 처리한다.
 * - N+1 문제 방지를 위해 유저 정보를 Bulk 조회하며, 개별 발급 트랜잭션을 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueListener {

    private final RedissonClient redissonClient;
    private final CouponIssueBatchWorker couponIssueBatchWorker;

    private static final int BATCH_SIZE = 100;

    /**
     * 주기적으로 활성 큐 목록을 확인하여 쿠폰 발급 대기열을 처리한다.
     *
     * - 1초마다 실행 (시스템 부하 방지 - 고정 지연)
     */
    @Scheduled(fixedDelay = 1000)
    public void run() {
        RSet<String> activeQueues = redissonClient.getSet(CouponRedisSchema.getActiveQueuesKey());
        if (activeQueues.isEmpty()) {
            return;
        }

        activeQueues.forEach(id -> {
            try {
                processQueue(Long.parseLong(id));
            } catch (Exception e) {
                log.error("⛔ [Coupon-Worker] 큐 처리 중 시스템 오류 발생 | couponId={}, error={}", id, e.getMessage());
            }
        });
    }

    /**
     * 특정 쿠폰 큐가 비어 있을 때까지 BATCH_SIZE 단위로 데이터 처리를 위임한다.
     *
     * - 큐에 쌓인 잔여 데이터를 즉시 모두 처리
     */
    private void processQueue(Long couponId) {
        String waitingKey = CouponRedisSchema.getWaitingKey(couponId);
        RScoredSortedSet<String> waitingQueue = redissonClient.getScoredSortedSet(waitingKey);

        while (true) {
            Collection<ScoredEntry<String>> batch = waitingQueue.entryRange(0, BATCH_SIZE - 1);
            if (batch == null || batch.isEmpty()) {
                break;
            }

            couponIssueBatchWorker.issueBatch(couponId, batch);

            List<String> processedValues = batch.stream().map(ScoredEntry::getValue).toList();
            waitingQueue.removeAll(processedValues);
        }
    }

}
