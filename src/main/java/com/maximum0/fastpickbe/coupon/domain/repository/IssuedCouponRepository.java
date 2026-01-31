package com.maximum0.fastpickbe.coupon.domain.repository;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import com.maximum0.fastpickbe.coupon.domain.model.IssuedCoupon;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponListRequest;
import com.maximum0.fastpickbe.user.domain.model.User;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssuedCouponRepository {

    /**
     * 특정 사용자가 해당 쿠폰을 이미 발급받았는지 여부를 확인한다
     *
     * @param user   사용자 엔티티
     * @param coupon 쿠폰 엔티티
     * @return 발급 이력이 존재하면 true
     */
    boolean existsByUserAndCoupon(User user, Coupon coupon);

    /**
     * 발급 이력을 저장한다
     *
     * @param issuedCoupon 발급 쿠폰 엔티티
     * @return 저장된 발급 쿠폰 엔티티
     */
    IssuedCoupon save(IssuedCoupon issuedCoupon);

    /**
     * 특정 쿠폰의 총 발급 수량을 조회한다
     *
     * @param couponId 쿠폰 식별자
     * @return 해당 쿠폰의 발급 건수
     */
    long countByCouponId(Long couponId);

    /**
     * 전체 발급된 쿠폰 수량을 조회한다
     *
     * @return 시스템 전체 발급 건수
     */
    long count();

    /**
     * 특정 사용자의 발급 쿠폰 목록을 검색 조건에 따라 페이징 조회한다
     *
     * @param user     조회할 사용자 엔티티
     * @param request  검색 키워드 및 상태 필터를 포함한 요청 객체
     * @param pageable 페이징 정보
     * @param now      현재 기준 시각
     * @return 페이징 처리된 발급 쿠폰 목록
     */
    Page<IssuedCoupon> findAllByUser(User user, MyCouponListRequest request, Pageable pageable, LocalDateTime now);

    /**
     * 모든 발급 데이터를 일괄 삭제한다 (테스트 환경 전용)
     */
    void deleteAllInBatch();

}
