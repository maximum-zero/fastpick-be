package com.maximum0.fastpickbe.coupon.domain.model;

import com.maximum0.fastpickbe.common.domain.BaseCreateEntity;
import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponStatus;
import com.maximum0.fastpickbe.user.domain.model.User;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_issued_coupon")
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

    // --- 생성자 ---

    @Builder(access = AccessLevel.PRIVATE)
    private IssuedCoupon(Long id, User user, Coupon coupon, LocalDateTime usedAt) {
        this.id = id;
        this.user = user;
        this.coupon = coupon;
        this.usedAt = usedAt;
    }

    // --- 정적 팩토리 메서드 ---

    /**
     * 특정 유저에게 쿠폰을 발급 처리한다. (비즈니스 로직)
     */
    public static IssuedCoupon create(User user, Coupon coupon) {
        return IssuedCoupon.builder()
                .user(user)
                .coupon(coupon)
                .build();
    }

    /**
     * 특정 유저에게 쿠폰을 발급 처리한다. (테스트 코드)
     */
    public static IssuedCoupon forTest(Long id, User user, Coupon coupon, LocalDateTime usedAt) {
        return IssuedCoupon.builder()
                .id(id)
                .user(user)
                .coupon(coupon)
                .usedAt(usedAt)
                .build();
    }

    // --- 비즈니스 행위 로직 ---

    /**
     * 발급된 쿠폰을 사용 처리한다.
     * @param now 사용 시점
     */
    public void use(LocalDateTime now) {
        this.usedAt = now;
    }


    // --- 상태 판별 및 계산 로직 ---

    /**
     * 현재 발급된 쿠폰의 상태를 계산한다.
     * @param now 기준 시각
     * @return 쿠폰 상태 (AVAILABLE, USED, EXPIRED)
     */
    public IssuedCouponStatus calculateStatus(LocalDateTime now) {
        if (isUsed()) {
            return IssuedCouponStatus.USED;
        }

        if (coupon.isExpired(now)) {
            return IssuedCouponStatus.EXPIRED;
        }

        return IssuedCouponStatus.AVAILABLE;
    }

    /**
     * 쿠폰 사용 완료 여부를 확인한다.
     */
    public boolean isUsed() {
        return usedAt != null;
    }

}
