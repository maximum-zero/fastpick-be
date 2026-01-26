package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.common.domain.BaseCreateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tb_coupon_keyword",
        indexes = {
                @Index(
                        name = "idx_coupon_keyword_composite",
                        columnList = "keyword, couponId DESC"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponKeyword extends BaseCreateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Builder(access = AccessLevel.PRIVATE)
    private CouponKeyword(Long couponId, String keyword) {
        this.couponId = couponId;
        this.keyword = keyword;
    }

    public static CouponKeyword create(Coupon coupon, String keyword) {
        return CouponKeyword.builder()
                .couponId(coupon.getId())
                .keyword(keyword)
                .build();
    }

    public static CouponKeyword forTest(Long couponId, String keyword) {
        return CouponKeyword.builder()
                .couponId(couponId)
                .keyword(keyword)
                .build();
    }

}
