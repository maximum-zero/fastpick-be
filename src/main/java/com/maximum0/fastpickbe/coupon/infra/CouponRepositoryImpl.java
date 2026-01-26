package com.maximum0.fastpickbe.coupon.infra;

import static com.maximum0.fastpickbe.coupon.domain.QCoupon.coupon;

import com.maximum0.fastpickbe.coupon.domain.Coupon;
import com.maximum0.fastpickbe.coupon.domain.CouponRepository;
import com.maximum0.fastpickbe.coupon.domain.CouponUseStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
     * 제공된 식별자 목록에 해당하는 모든 쿠폰 엔티티를 조회합니다.
     * @param ids 쿠폰 식별자 리스트
     * @return 조회된 쿠폰 엔티티 목록 (데이터가 없거나 ids가 비어있을 경우 빈 리스트 반환)
     */
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
}
