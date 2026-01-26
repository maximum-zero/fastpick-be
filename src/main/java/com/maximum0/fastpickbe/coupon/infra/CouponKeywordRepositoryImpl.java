package com.maximum0.fastpickbe.coupon.infra;

import static com.maximum0.fastpickbe.coupon.domain.QCouponKeyword.couponKeyword;
import static io.jsonwebtoken.lang.Strings.hasText;

import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.CouponKeyword;
import com.maximum0.fastpickbe.coupon.domain.CouponKeywordRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponUseStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
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
    private final JpaCouponKeywordRepository couponKeywordRepository;

    /**
     * 쿠폰 키워드 엔티티 목록을 일괄 저장합니다.
     * @param couponKeywordList 저장할 쿠폰 키워드 엔티티 목록
     */
    @Override
    public void saveAll(List<CouponKeyword> couponKeywordList) {
        couponKeywordRepository.saveAll(couponKeywordList);
    }

    /**
     * 검색 조건에 부합하는 쿠폰 식별자(ID) 목록을 페이징하여 조회합니다.
     * @param request 검색 키워드 및 필터 타입을 포함한 요청 객체
     * @param pageable 페이징 정보 (Offset, Limit)
     * @param now 현재 기준 시각
     * @return 검색 조건에 부합하는 쿠폰 식별자 리스트
     */
    @Override
    public List<Long> findIdsByCondition(CouponListRequest request, Pageable pageable, LocalDateTime now) {
        return queryFactory
                .select(couponKeyword.couponId)
                .from(couponKeyword)
                .where(
                        keywordStartsWith(request.search()),
                        filterTypeEq(request.filterType(), now),
                        couponKeyword.useStatus.eq(CouponUseStatus.AVAILABLE.name())
                )
                .orderBy(couponKeyword.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(couponKeyword.createdAt.desc())
                .fetch();
    }

    /**
     * 검색 조건에 부합하는 전체 쿠폰 개수를 조회합니다.
     * @param request 검색 키워드 및 필터 타입을 포함한 요청 객체
     * @param now 현재 기준 시각
     * @return 검색 조건에 부합하는 전체 데이터 건수
     */
    @Override
    public Long countByCondition(CouponListRequest request, LocalDateTime now) {
        return queryFactory
                .select(couponKeyword.count())
                .from(couponKeyword)
                .where(
                        keywordStartsWith(request.search()),
                        filterTypeEq(request.filterType(), now),
                        couponKeyword.useStatus.eq(CouponUseStatus.AVAILABLE.name())
                )
                .fetchOne();
    }

    private BooleanExpression keywordStartsWith(String keyword) {
        return hasText(keyword) ? couponKeyword.keyword.startsWith(keyword) : null;
    }

    /**
     * 쿠폰 상태 필터링을 위한 조건을 생성합니다.
     * @param filterType 필터 타입 (READY, ISSUING, CLOSED 등)
     * @param now 현재 기준 시각
     * @return 필터 타입에 대응하는 조건식
     */
    private BooleanExpression filterTypeEq(CouponFilterType filterType, LocalDateTime now) {
        if (filterType == null) return null;

        return switch (filterType) {
            case ALL -> null;
            case READY -> couponKeyword.startAt.after(now);
            case ISSUING -> couponKeyword.startAt.loe(now)
                    .and(couponKeyword.endAt.after(now))
                    .and(couponKeyword.isSoldOut.isFalse());
            case CLOSED -> couponKeyword.endAt.loe(now)
                    .or(couponKeyword.isSoldOut.isTrue());
        };
    }
}
