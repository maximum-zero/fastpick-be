package com.maximum0.fastpickbe.coupon.infra.repository;

import static com.maximum0.fastpickbe.coupon.domain.model.QCoupon.coupon;
import static com.maximum0.fastpickbe.coupon.domain.model.QIssuedCoupon.issuedCoupon;
import static org.springframework.util.StringUtils.hasText;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.repository.IssuedCouponRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponFilterType;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.user.domain.model.User;
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

    private final IssuedCouponJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

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

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return jpaRepository.save(issuedCoupon);
    }

    @Override
    public long countByCouponId(Long couponId) {
        Long count = queryFactory
                .select(issuedCoupon.count())
                .from(issuedCoupon)
                .where(issuedCoupon.coupon.id.eq(couponId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Page<IssuedCoupon> findAllByUser(User user, MyCouponListRequest request, Pageable pageable, LocalDateTime now) {
        List<IssuedCoupon> content = queryFactory
                .selectFrom(issuedCoupon)
                .join(issuedCoupon.coupon, coupon).fetchJoin()
                .where(
                        issuedCoupon.user.eq(user),
                        containsTitle(request.search()),
                        eqStatus(request.status(), now)
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
                        containsTitle(request.search()),
                        eqStatus(request.status(), now)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public void deleteAllInBatch() {
        jpaRepository.deleteAllInBatch();
    }

    // --- 내부 필터 조건 메서드 ---

    /**
     * 쿠폰 제목 포함 조건 표현식을 생성한다
     *
     * @param title 검색할 제목 키워드
     * @return 제목 포함 조건 표현식
     */
    private BooleanExpression containsTitle(String title) {
        return hasText(title) ? coupon.title.contains(title) : null;
    }

    /**
     * 필터 타입에 따른 발급 쿠폰 상태 조건 표현식을 생성한다
     *
     * @param status 필터링할 쿠폰 상태
     * @param now    현재 기준 시각
     * @return 상태 조건 표현식
     */
    private BooleanExpression eqStatus(IssuedCouponFilterType status, LocalDateTime now) {
        if (status == null || status == IssuedCouponFilterType.ALL) {
            return null;
        }
        return switch (status) {
            case AVAILABLE -> issuedCoupon.usedAt.isNull()
                    .and(coupon.endAt.after(now));
            case USED -> issuedCoupon.usedAt.isNotNull();
            case EXPIRED -> issuedCoupon.usedAt.isNotNull()
                    .or(coupon.endAt.before(now));
            default -> null;
        };
    }

}
