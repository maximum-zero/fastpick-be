package com.maximum0.fastpickbe.coupon.infra;

import static com.maximum0.fastpickbe.coupon.domain.QIssuedCoupon.issuedCoupon;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.domain.IssuedCouponRepository;
import com.maximum0.fastpickbe.user.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
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

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void deleteAllInBatch() {
        jpaRepository.deleteAllInBatch();
    }
}