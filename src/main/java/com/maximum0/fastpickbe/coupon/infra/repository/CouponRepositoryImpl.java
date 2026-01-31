package com.maximum0.fastpickbe.coupon.infra.repository;

import static com.maximum0.fastpickbe.coupon.domain.model.QCoupon.coupon;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.repository.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Coupon save(Coupon coupon) {
        return jpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findActiveById(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(coupon)
                        .where(
                                coupon.id.eq(id),
                                isAvailable()
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<Coupon> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .selectFrom(coupon)
            .where(coupon.id.in(ids))
            .fetch();
    }

    @Override
    public void deleteAllInBatch() {
        jpaRepository.deleteAllInBatch();
    }

    // --- 내부 필터 조건 메서드 ---

    /**
     * 사용 가능한 상태인지 확인하는 표현식을 생성한다.
     *
     * @return 쿠폰 사용 가능 상태 조건 표현식
     */
    private BooleanExpression isAvailable() {
        return coupon.useStatus.eq(CouponUseStatus.AVAILABLE);
    }

}
