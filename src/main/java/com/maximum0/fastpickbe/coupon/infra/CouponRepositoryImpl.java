package com.maximum0.fastpickbe.coupon.infra;

import static com.maximum0.fastpickbe.coupon.domain.QCoupon.coupon;
import static org.springframework.util.StringUtils.hasText;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponFilterType;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponUseStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final JpaCouponRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    /**
     * 쿠폰을 저장합니다.
     * @param coupon 저장할 쿠폰 엔티티
     * @return 저장된 쿠폰 엔티티
     */
    @Override
    public Coupon save(Coupon coupon) {
        return jpaRepository.save(coupon);
    }

    /**
     * 활성화된 쿠폰을 조회합니다.
     * @param id 쿠폰 식별자
     * @return 중단(DISABLED)되지 않은 쿠폰의 Optional 객체
     */
    @Override
    public Optional<Coupon> findActiveById(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(coupon)
                        .where(
                                coupon.id.eq(id),
                                coupon.useStatus.ne(CouponUseStatus.DISABLED)
                        )
                        .fetchOne()
        );
    }

    /**
     * 비관적 락을 적용하여 쿠폰 엔티티를 조회합니다.
     * @param id 쿠폰 식별자
     * @return 쿠폰 엔티티 (Optional)
     */
    @Override
    public Optional<Coupon> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id);
    }

    /**
     * 검색 조건과 페이징 설정에 따라 쿠폰 목록을 조회합니다.
     * @param request 검색 조건 (제목, 필터 타입)
     * @param pageable 페이징 정보 (offset, limit, sort)
     * @param now 필터링 기준 시각
     * @return 페이징 처리된 쿠폰 요약 정보 목록
     */
    @Override
    public Page<CouponSummaryResponse> findAll(CouponListRequest request, Pageable pageable, LocalDateTime now) {
        List<Coupon> content = queryFactory
                .selectFrom(coupon)
                .where(
                        titleContains(request.search()),
                        filterTypeEq(request.filterType(), now),
                        coupon.useStatus.ne(CouponUseStatus.DISABLED)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(coupon.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(coupon.count())
                .from(coupon)
                .where(
                        titleContains(request.search()),
                        filterTypeEq(request.filterType(), now),
                        coupon.useStatus.ne(CouponUseStatus.DISABLED)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne)
                .map(c -> CouponSummaryResponse.from(c, now));
    }

    @Override
    public void deleteAllInBatch() {
        jpaRepository.deleteAllInBatch();
    }

    /**
     * 쿠폰 제목에 대한 부분 일치 조건을 생성합니다.
     * @param title 검색할 제목 키워드
     * @return 제목 포함 조건식 (키워드가 비어있을 경우 null 반환)
     */
    private BooleanExpression titleContains(String title) {
        return hasText(title) ? coupon.title.contains(title) : null;
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
            case READY -> coupon.startAt.after(now);
            case ISSUING -> coupon.startAt.loe(now)
                    .and(coupon.endAt.after(now))
                    .and(coupon.issuedQuantity.lt(coupon.totalQuantity));
            case CLOSED -> coupon.endAt.loe(now)
                    .or(coupon.issuedQuantity.goe(coupon.totalQuantity));
        };
    }
}
