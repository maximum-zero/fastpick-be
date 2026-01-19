package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.common.domain.BaseCreateEntity;
import com.maximum0.fastpickbe.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tb_issued_coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = true)
    private LocalDateTime usedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private IssuedCoupon(User user, Coupon coupon) {
        this.user = user;
        this.coupon = coupon;
    }

    /**
     * 특정 유저에게 쿠폰을 발급 처리합니다.
     * @param user 발급 대상 유저
     * @param coupon 발급될 쿠폰
     * @return IssuedCoupon 발급된 쿠폰 객체
     */
    public static IssuedCoupon create(User user, Coupon coupon) {
        return IssuedCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();
    }

    /**
     * 발급된 쿠폰을 사용 처리합니다.
     * @param now 사용 시점
     */
    public void use(LocalDateTime now) {
        this.usedAt = now;
    }

    /**
     * 쿠폰 사용 완료 여부를 확인합니다.
     * @return 사용 완료 시 true
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * 현재 발급된 쿠폰의 상태를 동적으로 계산합니다.
     * @param now 기준 시간
     * @return 쿠폰 상태 (AVAILABLE, USED, EXPIRED)
     */
    public MyCouponStatus calculateStatus(LocalDateTime now) {
        if (isUsed()) {
            return MyCouponStatus.USED;
        }
        if (coupon.isExpired(now)) {
            return MyCouponStatus.EXPIRED;
        }
        return MyCouponStatus.AVAILABLE;
    }

}
