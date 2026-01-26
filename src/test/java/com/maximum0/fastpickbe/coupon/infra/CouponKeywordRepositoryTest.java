package com.maximum0.fastpickbe.coupon.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.CouponKeyword;
import com.maximum0.fastpickbe.coupon.domain.CouponStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
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
    @DisplayName("기본 상태 필터링 검증")
    class BasicFilterTest {

        @BeforeEach
        void setUp() {
            // ISSUING: 진행 중인 나이키 쿠폰
            saveCouponData("나이키1", "AVAILABLE", now.minusDays(1), now.plusDays(1), 100, 0, false);

            // READY: 내일부터 시작하는 나이키 쿠폰
            saveCouponData("나이키2", "AVAILABLE", now.plusDays(1), now.plusDays(2), 100, 0, false);

            // EXPIRED: 이미 종료된 나이키 쿠폰
            saveCouponData("나이키3", "AVAILABLE", now.minusDays(2), now.minusDays(1), 100, 0, false);

            // EXHAUSTED: 진행 중이지만 품절된 나이키 쿠폰
            saveCouponData("나이키4", "AVAILABLE", now.minusDays(1), now.plusDays(1), 100, 100, true);

            // DISABLED: 비활성화된 나이키 쿠폰
            saveCouponData("나이키5", "DISABLED", now.minusDays(1), now.plusDays(1), 100, 0, false);

            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("ISSUING 필터 적용 시 현재 발행 기간 내에 있고 품절되지 않은 쿠폰을 반환한다")
        void findAllByCondition_ReturnsOnlyIssuing() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.ISSUING);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(CouponStatus.ISSUING.name());
        }

        @Test
        @DisplayName("CLOSED 필터 적용 시 기간이 만료되었거나 품절된 쿠폰을 반환한다")
        void findAllByCondition_ReturnsClosedOrSoldOut() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.CLOSED);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).status()).isEqualTo(CouponStatus.EXHAUSTED.name());
            assertThat(result.get(1).status()).isEqualTo(CouponStatus.EXPIRED.name());
        }

        @Test
        @DisplayName("READY 필터 적용 시 아직 시작되지 않은 쿠폰을 반환한다")
        void findAllByCondition_ReturnsReady() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.READY);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(CouponStatus.READY.name());
        }
    }

    @Nested
    @DisplayName("복합 조건 및 경계값 검증 테스트")
    class ComplexConditionAndBoundaryTest {

        @BeforeEach
        void setUp() {
            // 현재 시작하는 쿠폰
            saveCouponData("나이키", "AVAILABLE", now, now.plusDays(1), 100, 0, false);
            // 현재 끝나는 쿠폰
            saveCouponData("나이키", "AVAILABLE", now.minusDays(1), now, 100, 0, false);
            // 다른 브랜드 쿠폰
            saveCouponData("아디다스", "AVAILABLE", now.minusDays(1), now.plusDays(1), 100, 0, false);
        }

        @Test
        @DisplayName("시작 시간이 현재 시간과 일치하면 ISSUING 상태로 간주한다")
        void shouldBeIssuing_WhenStartAtEqualsNow() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.ISSUING);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).anyMatch(c -> c.status().equals(CouponStatus.ISSUING.name()));
        }

        @Test
        @DisplayName("브랜드 검색어와 필터를 동시에 적용하면 교집합 데이터만 반환한다")
        void shouldReturnIntersection_WhenSearchAndFilterApplied() {
            // given
            // 아디다스도 ISSUING 상태지만, 검색어는 "나이키"인 상황
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.ISSUING);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).allMatch(c -> c.brand().equals("나이키"));
            assertThat(result).noneMatch(c -> c.brand().equals("아디다스"));
        }
    }


    private void saveCouponData(String brand, String useStatus, LocalDateTime start, LocalDateTime end, int totalQuantity, int issuedQuantity, boolean isSoldOut) {
        Coupon coupon = Coupon.forTest(null, brand, brand + " 제목", "요약 설명", "상세 설명", totalQuantity, issuedQuantity, start, end);
        em.persist(coupon);

        CouponKeyword couponKeyword = CouponKeyword.forTest(coupon.getId(), brand, useStatus, start, end, isSoldOut);
        em.persist(couponKeyword);
    }
}