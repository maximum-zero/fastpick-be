package com.maximum0.fastpickbe.coupon.infra.repository;

import com.maximum0.fastpickbe.coupon.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.issuedQuantity = c.issuedQuantity + :count WHERE c.id = :id")
    void incrementIssuedQuantity(@Param("id") Long id, @Param("count") int count);

}