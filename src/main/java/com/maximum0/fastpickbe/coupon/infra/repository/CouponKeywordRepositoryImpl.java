package com.maximum0.fastpickbe.coupon.infra.repository;

import static com.maximum0.fastpickbe.coupon.domain.model.QCoupon.coupon;
import static com.maximum0.fastpickbe.coupon.domain.model.QCouponKeyword.couponKeyword;
import static com.maximum0.fastpickbe.coupon.infra.repository.query.CouponExpressions.toCouponStatus;
import static io.jsonwebtoken.lang.Strings.hasText;

import com.maximum0.fastpickbe.coupon.domain.model.CouponKeyword;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import com.maximum0.fastpickbe.coupon.infra.repository.query.CouponExpressions;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponKeywordRepositoryImpl implements CouponKeywordRepository {

    private final JPAQueryFactory queryFactory;
    private final CouponKeywordJpaRepository couponKeywordRepository;

    @Override
    public List<CouponSummaryResponse> findAllByCondition(CouponListRequest request, Pageable pageable, LocalDateTime now) {
        return queryFactory
                .select(Projections.constructor(CouponSummaryResponse.class,
                        coupon.id,
                        coupon.brand,
                        coupon.title,
                        coupon.summary,
                        coupon.totalQuantity,
                        coupon.issuedQuantity,
                        coupon.startAt,
                        coupon.endAt,
                        toCouponStatus(now)
                ))
                .from(couponKeyword)
                .innerJoin(coupon).on(couponKeyword.couponId.eq(coupon.id))
                .where(
                        keywordStartsWith(request.search()),
                        eqFilterType(request.filterType(), now),
                        coupon.useStatus.eq(CouponUseStatus.AVAILABLE)
                )
                .orderBy(coupon.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public Long countByCondition(CouponListRequest request, LocalDateTime now) {
        return queryFactory
                .select(couponKeyword.count())
                .from(couponKeyword)
                .innerJoin(coupon).on(couponKeyword.couponId.eq(coupon.id))
                .where(
                        keywordStartsWith(request.search()),
                        eqFilterType(request.filterType(), now),
                        coupon.useStatus.eq(CouponUseStatus.AVAILABLE)
                )
                .fetchOne();
    }

    @Override
    public void saveAll(List<CouponKeyword> couponKeywordList) {
        couponKeywordRepository.saveAll(couponKeywordList);
    }

    // --- 내부 필터 조건 메서드 ---

    /**
     * 키워드 시작 조건에 따른 표현식을 생성한다
     *
     * @param keyword 검색할 키워드
     * @return 키워드 시작 조건 표현식 (키워드 공백 시 null 반환)
     */
    private BooleanExpression keywordStartsWith(String keyword) {
        return hasText(keyword) ? couponKeyword.keyword.startsWith(keyword) : null;
    }

    /**
     * 쿠폰 상태 필터 타입에 따른 표현식을 생성한다.
     *
     * @param filterType 필터 타입 (READY, ISSUING, CLOSED 등)
     * @param now        현재 기준 시각
     * @return 필터 타입에 대응하는 조건 표현식
     */
    private BooleanExpression eqFilterType(CouponFilterType filterType, LocalDateTime now) {
        if (filterType == null) return null;

        return switch (filterType) {
            case ALL -> null;
            case READY -> CouponExpressions.isReady(now);
            case ISSUING -> CouponExpressions.isIssuing(now);
            case CLOSED -> CouponExpressions.isExpired(now)
                    .or(CouponExpressions.isExhausted());
        };
    }

}
