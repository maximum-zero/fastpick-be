package com.maximum0.fastpickbe.coupon.infra;

import static com.maximum0.fastpickbe.coupon.domain.QCoupon.coupon;
import static com.maximum0.fastpickbe.coupon.domain.QIssuedCoupon.issuedCoupon;
import static org.springframework.util.StringUtils.hasText;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatusFilter;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.user.domain.User;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IssuedCouponRepositoryImpl implements IssuedCouponRepository {
    private final JpaIssuedCouponRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자가 특정 쿠폰을 이미 발급받았는지 확인합니다.
     * @return 존재하면 true, 없으면 false
     */
    @Override
    public boolean existsByUserAndCoupon(User user, Coupon coupon) {
        return queryFactory
                .selectOne()
                .from(issuedCoupon)
                .where(
                        issuedCoupon.user.eq(user),
                        issuedCoupon.coupon.eq(coupon)
                )
                .fetchFirst() != null;
    }

    /**
     * 발급 이력을 저장합니다.
     */
    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return jpaRepository.save(issuedCoupon);
    }

    /**
     * 특정 쿠폰 ID에 해당하는 발급된 쿠폰의 수를 조회합니다.
     */
    @Override
    public long countByCouponId(Long couponId) {
        Long count = queryFactory
                .select(issuedCoupon.count())
                .from(issuedCoupon)
                .where(issuedCoupon.coupon.id.eq(couponId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 전체 발급된 쿠폰 수를 조회합니다.
     */
    @Override
    public long count() {
        return jpaRepository.count();
    }

    /**
     * 모든 발급된 쿠폰을 일괄 삭제합니다. (테스트용)
     */
    @Override
    public void deleteAllInBatch() {
        jpaRepository.deleteAllInBatch();
    }

    /**
     * 특정 사용자의 쿠폰 목록을 필터링 조건과 함께 페이징하여 조회합니다.
     * @param user 조회할 사용자
     * @param request 쿠폰 목록 조회 요청 DTO (검색어, 상태 필터 포함)
     * @param pageable 페이징 정보
     * @param now 현재 시간
     * @return 페이징된 발급 쿠폰 목록
     */
    @Override
    public Page<IssuedCoupon> findAllByUser(User user, MyCouponListRequest request, Pageable pageable, LocalDateTime now) {
        List<IssuedCoupon> content = queryFactory
                .selectFrom(issuedCoupon)
                .join(issuedCoupon.coupon, coupon).fetchJoin()
                .where(
                        issuedCoupon.user.eq(user),
                        titleContains(request.search()),
                        statusFilter(request.status(), now)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(issuedCoupon.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(issuedCoupon.count())
                .from(issuedCoupon)
                .join(issuedCoupon.coupon, coupon)
                .where(
                        issuedCoupon.user.eq(user),
                        titleContains(request.search()),
                        statusFilter(request.status(), now)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
     * @param status 필터링할 쿠폰 상태 (ALL, AVAILABLE, USED, EXPIRED)
     * @param now 현재 기준 시각
     * @return 필터 타입에 대응하는 조건식
     */
    private BooleanExpression statusFilter(MyCouponStatusFilter status, LocalDateTime now) {
        if (status == null || status == MyCouponStatusFilter.ALL) {
            return null;
        }
        if (status == MyCouponStatusFilter.AVAILABLE) {
            return issuedCoupon.usedAt.isNull()
                    .and(coupon.endAt.after(now));
        }
        if (status == MyCouponStatusFilter.USED) {
            return issuedCoupon.usedAt.isNotNull();
        }
        if (status == MyCouponStatusFilter.EXPIRED) {
            return issuedCoupon.usedAt.isNotNull()
                    .or(coupon.endAt.before(now));
        }
        return null;
    }
}