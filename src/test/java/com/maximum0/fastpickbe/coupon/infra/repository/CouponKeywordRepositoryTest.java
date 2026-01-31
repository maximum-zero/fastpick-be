package com.maximum0.fastpickbe.coupon.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.common.config.JpaConfig;
import com.maximum0.fastpickbe.common.config.QuerydslConfig;
import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.CouponKeyword;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponStatus;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import com.maximum0.fastpickbe.coupon.infra.repository.CouponKeywordRepositoryImpl;
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
@DisplayName("Coupon Keyword Repository 슬라이스 테스트")
class CouponKeywordRepositoryTest {

    @Autowired
    private CouponKeywordRepository couponKeywordRepository;

    @Autowired
    private EntityManager em;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("기본 상태 필터링 시나리오 테스트")
    class Basic_Filter_Scenario {

        @BeforeEach
        void setUp() {
            // ISSUING: 진행 중인 나이키 쿠폰
            saveCouponData("나이키1", now.minusDays(1), now.plusDays(1), 100, 0, CouponUseStatus.AVAILABLE);

            // READY: 내일부터 시작하는 나이키 쿠폰
            saveCouponData("나이키2", now.plusDays(1), now.plusDays(2), 100, 0, CouponUseStatus.AVAILABLE);

            // EXPIRED: 이미 종료된 나이키 쿠폰
            saveCouponData("나이키3", now.minusDays(2), now.minusDays(1), 100, 0, CouponUseStatus.AVAILABLE);

            // EXHAUSTED: 진행 중이지만 품절된 나이키 쿠폰
            saveCouponData("나이키4", now.minusDays(1), now.plusDays(1), 100, 100, CouponUseStatus.AVAILABLE);

            // DISABLED: 비활성화된 나이키 쿠폰
            saveCouponData("나이키5", now.minusDays(1), now.plusDays(1), 100, 0, CouponUseStatus.DISABLED);

            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("ISSUING 필터 적용 시 현재 발행 기간 내에 있고 품절되지 않은 쿠폰을 반환한다")
        void givenIssuingFilter_whenFindAllByCondition_thenReturnsOnlyIssuing() {
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
        void givenClosedFilter_whenFindAllByCondition_thenReturnsClosedOrSoldOut() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.CLOSED);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CouponSummaryResponse::status)
                    .containsExactlyInAnyOrder(CouponStatus.EXHAUSTED.name(), CouponStatus.EXPIRED.name());
        }

        @Test
        @DisplayName("READY 필터 적용 시 아직 시작되지 않은 쿠폰을 반환한다")
        void givenReadyFilter_whenFindAllByCondition_thenReturnsReady() {
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
    @DisplayName("복합 조건 및 경계값 검증 시나리오 테스트")
    class Complex_Condition_Scenario {

        @BeforeEach
        void setUp() {
            // 현재 시작하는 쿠폰
            saveCouponData("나이키", now, now.plusDays(1), 100, 0, CouponUseStatus.AVAILABLE);
            // 현재 끝나는 쿠폰
            saveCouponData("나이키", now.minusDays(1), now, 100, 0, CouponUseStatus.AVAILABLE);
            // 다른 브랜드 쿠폰
            saveCouponData("아디다스", now.minusDays(1), now.plusDays(1), 100, 0, CouponUseStatus.AVAILABLE);
        }

        @Test
        @DisplayName("시작 시간이 현재 시간과 일치하면 ISSUING 상태로 간주한다")
        void givenStartAtEqualsNow_whenFindAllByCondition_thenReturnsIssuing() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.ISSUING);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).anyMatch(c -> c.status().equals(CouponStatus.ISSUING.name()));
        }

        @Test
        @DisplayName("브랜드 검색어와 필터를 동시에 적용하면 교집합 데이터만 반환한다")
        void givenSearchAndFilter_whenFindAllByCondition_thenReturnsIntersection() {
            // given
            CouponListRequest request = new CouponListRequest("나이키", CouponFilterType.ISSUING);

            // when
            List<CouponSummaryResponse> result = couponKeywordRepository.findAllByCondition(request, PageRequest.of(0, 10), now);

            // then
            assertThat(result).allMatch(c -> c.brand().equals("나이키"));
            assertThat(result).noneMatch(c -> c.brand().equals("아디다스"));
        }
    }

    // --- 테스트 메서드 ---

    private void saveCouponData(String brand, LocalDateTime start, LocalDateTime end, int total, int issued, CouponUseStatus useStatus) {
        Coupon coupon = Coupon.forTest(null, brand, brand + " 제목", "요약", "상세", total, issued, start, end, useStatus);
        em.persist(coupon);

        CouponKeyword couponKeyword = CouponKeyword.forTest(coupon.getId(), brand);
        em.persist(couponKeyword);
    }

}
