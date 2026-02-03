package com.maximum0.fastpickbe.coupon.domain.repository;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    /**
     * 쿠폰 정보를 저장한다
     *
     * @param coupon 저장할 쿠폰 엔티티
     * @return 저장된 쿠폰 엔티티
     */
    Coupon save(Coupon coupon);

    /**
     * 활성화된 쿠폰 정보를 조회한다
     *
     * @param id 쿠폰 식별자
     * @return 중단(DISABLED)되지 않은 쿠폰 엔티티 (존재하지 않을 경우 Empty)
     */
    Optional<Coupon> findActiveById(Long id);

    /**
     * 식별자 목록에 해당하는 모든 쿠폰 엔티티를 조회한다
     *
     * @param ids 쿠폰 식별자 리스트
     * @return 조회된 쿠폰 엔티티 목록 (데이터가 없거나 ids가 비어있을 경우 빈 리스트 반환)
     */
    List<Coupon> findAllByIds(List<Long> ids);

    /**
     * 특정 쿠폰의 누적 발급 수량을 일괄 업데이트한다.
     *
     * @param id    수량을 업데이트할 쿠폰의 식별자
     * @param count 증가시킬 발급 수량
     */
    void incrementIssuedQuantity(Long id, int count);

    /**
     * 모든 쿠폰 데이터를 일괄 삭제한다 (테스트 환경 전용)
     */
    void deleteAllInBatch();

}
