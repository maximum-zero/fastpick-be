package com.maximum0.fastpickbe.coupon.domain.model;

import com.maximum0.fastpickbe.common.domain.BaseCreateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tb_coupon_keyword",
        indexes = {
                @Index(
                        name = "idx_coupon_keyword_composite",
                        columnList = "keyword, couponId DESC"
                )
        }
)
public class CouponKeyword extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;  // 도메인 간 결합도를 낮추기 위해 식별자(ID)를 직접 참조한다.

    @Column(nullable = false, length = 100)
    private String keyword;

    // --- 생성자 ---

    @Builder(access = AccessLevel.PRIVATE)
    private CouponKeyword(Long couponId, String keyword) {
        this.couponId = couponId;
        this.keyword = keyword;
    }

    // --- 정적 팩토리 메서드 ---

    /**
     * 쿠폰 검색 키워드 객체를 생성한다. (비즈니스 로직)
     */
    public static CouponKeyword create(Coupon coupon, String keyword) {
        return CouponKeyword.builder()
                .couponId(coupon.getId())
                .keyword(keyword)
                .build();
    }

    /**
     * 쿠폰 검색 키워드 객체를 생성한다. (테스트 코드)
     */
    public static CouponKeyword forTest(Long couponId, String keyword) {
        return CouponKeyword.builder()
                .couponId(couponId)
                .keyword(keyword)
                .build();
    }

}
