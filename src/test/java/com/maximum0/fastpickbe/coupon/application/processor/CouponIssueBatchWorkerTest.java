package com.maximum0.fastpickbe.coupon.application.processor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.client.protocol.ScoredEntry;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@DisplayName("Coupon Issue Batch Worker 단위 테스트")
class CouponIssueBatchWorkerTest {

    @InjectMocks
    private CouponIssueBatchWorker couponIssueBatchWorker;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponIssueProcessor couponIssueProcessor;

    @Mock
    private Clock clock;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        given(clock.getZone()).willReturn(KST);

        testCoupon = Coupon.create("29CM", "배치 쿠폰", "요약", "상세", 100, now.minusDays(1), now.plusDays(1));
    }

    @Nested
    @DisplayName("배치 발급 실행(issueBatch) 테스트")
    class Issue_Batch {

        @Test
        @DisplayName("여러 유저의 발급 요청을 한 번에 처리하고 재고를 벌크로 업데이트한다")
        void givenMultipleUsers_whenIssueBatch_thenSuccessAndBulkUpdate() {
            // given
            Long couponId = 1L;
            double score = (double) now.atZone(KST).toInstant().toEpochMilli();

            // Redis에서 꺼내온 가짜 엔트리 2개
            ScoredEntry<String> entry1 = new ScoredEntry<>(score, "1");
            ScoredEntry<String> entry2 = new ScoredEntry<>(score, "2");
            List<ScoredEntry<String>> batch = List.of(entry1, entry2);

            User user1 = User.forTest(1L, "u1@test.com", "pw", "u1", UserRole.USER);
            User user2 = User.forTest(2L, "u2@test.com", "pw", "u2", UserRole.USER);

            given(couponRepository.findActiveById(couponId)).willReturn(Optional.of(testCoupon));
            given(userRepository.findAllById(any())).willReturn(List.of(user1, user2));

            // when
            couponIssueBatchWorker.issueBatch(couponId, batch);

            // then
            // Processor가 유저 수만큼 호출되었는지 확인
            verify(couponIssueProcessor, times(2)).execute(any(), any(), any());
            // 재고 업데이트가 벌크(2개)로 1번 호출되었는지 확인
            verify(couponRepository, times(1)).incrementIssuedQuantity(couponId, 2);
        }

        @Test
        @DisplayName("일부 유저 처리 중 예외가 발생해도 나머지 유저는 정상 처리되어야 한다")
        void givenPartialFailure_whenIssueBatch_thenProcessRemainingUsers() {
            // given
            Long couponId = 1L;
            double score = (double) now.atZone(KST).toInstant().toEpochMilli();
            ScoredEntry<String> entry1 = new ScoredEntry<>(score, "1");
            ScoredEntry<String> entry2 = new ScoredEntry<>(score, "2");
            List<ScoredEntry<String>> batch = List.of(entry1, entry2);

            User user1 = User.forTest(1L, "u1@test.com", "pw", "u1", UserRole.USER);
            User user2 = User.forTest(2L, "u2@test.com", "pw", "u2", UserRole.USER);

            given(couponRepository.findActiveById(couponId)).willReturn(Optional.of(testCoupon));
            given(userRepository.findAllById(any())).willReturn(List.of(user1, user2));

            // 유저 1번은 발급 실패(RuntimeException)하도록 설정
            doThrow(new RuntimeException("DB Error")).when(couponIssueProcessor).execute(any(), eq(user1), any());

            // when
            couponIssueBatchWorker.issueBatch(couponId, batch);

            // then
            // 1명은 실패했지만 1명은 성공했으므로 increment값은 1이어야 함
            verify(couponRepository, times(1)).incrementIssuedQuantity(couponId, 1);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 요청하면 BusinessException을 던진다")
        void givenInvalidCouponId_whenIssueBatch_thenThrowsException() {
            // given
            given(couponRepository.findActiveById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponIssueBatchWorker.issueBatch(1L, List.of()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
        }

        @Test
        @DisplayName("Redis 대기열 유저가 DB에 존재하지 않으면 해당 유저는 스킵한다")
        void givenMissingUserInDb_whenIssueBatch_thenSkipsMissingUser() {
            // given
            Long couponId = 1L;
            ScoredEntry<String> entry1 = new ScoredEntry<>(0.0, "1"); // 존재하지 않을 유저
            ScoredEntry<String> entry2 = new ScoredEntry<>(0.0, "2"); // 존재하는 유저
            List<ScoredEntry<String>> batch = List.of(entry1, entry2);

            User user2 = User.forTest(2L, "u2@test.com", "pw", "u2", UserRole.USER);

            given(couponRepository.findActiveById(couponId)).willReturn(Optional.of(testCoupon));
            // 1번 유저는 리스트에 포함되지 않음 (DB 조회 실패 시뮬레이션)
            given(userRepository.findAllById(any())).willReturn(List.of(user2));

            // when
            couponIssueBatchWorker.issueBatch(couponId, batch);

            // then
            // 2번 유저에 대해서만 execute가 실행되었는지 확인
            verify(couponIssueProcessor, times(1)).execute(eq(testCoupon), eq(user2), any());
            // 실제 성공한 1명에 대해서만 재고 업데이트
            verify(couponRepository).incrementIssuedQuantity(couponId, 1);
        }

        @Test
        @DisplayName("모든 유저 발급에 실패하면 재고 업데이트를 호출하지 않는다")
        void givenAllFailures_whenIssueBatch_thenNoInventoryUpdate() {
            // given
            Long couponId = 1L;
            ScoredEntry<String> entry = new ScoredEntry<>(0.0, "1");
            List<ScoredEntry<String>> batch = List.of(entry);

            User user1 = User.forTest(1L, "u1@test.com", "pw", "u1", UserRole.USER);

            given(couponRepository.findActiveById(couponId)).willReturn(Optional.of(testCoupon));
            given(userRepository.findAllById(any())).willReturn(List.of(user1));

            // 전원 실패 모킹
            doThrow(new RuntimeException("Critical Error")).when(couponIssueProcessor).execute(any(), any(), any());

            // when
            couponIssueBatchWorker.issueBatch(couponId, batch);

            // then
            // 0건이므로 incrementIssuedQuantity가 호출되지 않아야 함
            verify(couponRepository, times(0)).incrementIssuedQuantity(anyLong(), anyInt());
        }
    }

}
