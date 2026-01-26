package com.maximum0.fastpickbe.coupon.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.CouponKeyword;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CouponKeywordRepositoryImpl.class, JpaConfig.class, QuerydslConfig.class})
@DisplayName("Coupon Keyword Repository 단위 테스트")
class CouponKeywordRepositoryTest {

    @Autowired
    private CouponKeywordRepositoryImpl couponKeywordRepository;

    @Autowired
    private EntityManager em;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("키워드 기반 식별자 추출 테스트")
    class FindIdsByConditionTest {

        @BeforeEach
        void setUp() {
            // ISSUING: 진행 중인 나이키 쿠폰
            em.persist(CouponKeyword.forTest(1L, "나이키", "AVAILABLE", now.minusDays(1), now.plusDays(1), false));

            // READY: 내일부터 시작하는 나이키 쿠폰
            em.persist(CouponKeyword.forTest(2L, "나이키", "AVAILABLE", now.plusDays(1), now.plusDays(2), false));

            // CLOSED: 이미 종료된 나이키 쿠폰
            em.persist(CouponKeyword.forTest(3L, "나이키", "AVAILABLE", now.minusDays(2), now.minusDays(1), false));

            // CLOSED: 진행 중이지만 품절(Sold Out)된 나이키 쿠폰
            em.persist(CouponKeyword.forTest(4L, "나이키", "AVAILABLE", now.minusDays(1), now.plusDays(1), true));

            // DISABLED: 키워드는 맞지만 비활성화된 나이키 쿠폰 (조회 제외 대상)
            em.persist(CouponKeyword.forTest(5L, "나이키", "DISABLED", now.minusDays(1), now.plusDays(1), false));

            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("ISSUING 필터 적용 시 현재 발행 기간 내에 있고 품절되지 않은 쿠폰 ID만 반환한다")
        void findIds_ReturnsOnlyIssuingIds() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.ISSUING);

            // when
            List<Long> result = couponKeywordRepository.findIdsByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(1L);
        }

        @Test
        @DisplayName("CLOSED 필터 적용 시 기간이 만료되었거나 품절된 쿠폰 ID를 반환한다")
        void findIds_ReturnsClosedOrSoldOutIds() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.CLOSED);

            // when
            List<Long> result = couponKeywordRepository.findIdsByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(4L, 3L); // 최신순 정렬
        }

        @Test
        @DisplayName("READY 필터 적용 시 아직 시작되지 않은 쿠폰 ID만 반환한다")
        void findIds_ReturnsReadyIds() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.READY);

            // when
            List<Long> result = couponKeywordRepository.findIdsByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(2L);
        }
    }

}