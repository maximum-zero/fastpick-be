package com.maximum0.fastpickbe.coupon.domain.model;

import com.maximum0.fastpickbe.common.domain.BaseEntity;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponStatus;
import com.maximum0.fastpickbe.coupon.domain.vo.CouponUseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_coupon")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String brand;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 200)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int limitPerUser;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponUseStatus useStatus;

    // --- 생성자 ---

    @Builder(access = AccessLevel.PRIVATE)
    public Coupon(Long id, String brand, String title, String summary, String description, int totalQuantity, int issuedQuantity, LocalDateTime startAt, LocalDateTime endAt, CouponUseStatus useStatus) {
        this.id = id;
        this.brand = brand;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.limitPerUser = 1;
        this.startAt = startAt;
        this.endAt = endAt;
        this.useStatus = useStatus;
    }

    // --- 정적 팩토리 메서드 ---

    /**
     * 쿠폰을 객체를 생성한다. (비즈니스 로직)
     */
    public static Coupon create(String brand, String title, String summary, String description, int totalQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.builder()
                .brand(brand)
                .title(title)
                .summary(summary)
                .description(description)
                .totalQuantity(totalQuantity)
                .issuedQuantity(0)
                .startAt(startAt)
                .endAt(endAt)
                .useStatus(CouponUseStatus.AVAILABLE)
                .build();
    }

    /**
     * 쿠폰을 객체를 생성한다. (테스트 코드)
     */
    public static Coupon forTest(Long id, String brand, String title, String summary, String description, int totalQuantity, int issuedQuantity, LocalDateTime startAt, LocalDateTime endAt, CouponUseStatus useStatus) {
        return Coupon.builder()
                .id(id)
                .brand(brand)
                .title(title)
                .summary(summary)
                .description(description)
                .totalQuantity(totalQuantity)
                .issuedQuantity(issuedQuantity)
                .startAt(startAt)
                .endAt(endAt)
                .useStatus(useStatus)
                .build();
    }

    // --- 비즈니스 행위 로직 ---

    /**
     * 쿠폰 발급 조건을 검증한다.
     * @param now 기준 시간
     * @throws BusinessException 발급 조건 미충족 시 발생
     */
    public void validateIssuable(LocalDateTime now) {
        if (this.useStatus == CouponUseStatus.DISABLED) {
            throw new BusinessException(ErrorCode.COUPON_DISABLED);
        }
        if (isReady(now) || isExpired(now)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE_PERIOD);
        }
        if (isExhausted()) {
            throw new BusinessException(ErrorCode.COUPON_EXHAUSTED);
        }
    }

    /**
     * 쿠폰을 발급한다.
     * 발급 조건을 검증한 후 발급 수량을 1 증가시킨다.
     * @param now 발급 시점
     */
    public void issue(LocalDateTime now) {
        validateIssuable(now);
        this.issuedQuantity++;
    }

    /**
     * 쿠폰을 비활성화(발급 중단) 처리한다.
     */
    public void disable() {
        this.useStatus = CouponUseStatus.DISABLED;
    }

    // --- 상태 판별 및 계산 로직 ---

    /**
     * 현재 쿠폰의 비즈니스 상태를 계산한다.
     * @param now 기준 시각
     * @return 쿠폰 상태 (READY, ISSUING, EXHAUSTED, EXPIRED, DISABLED)
     */
    public CouponStatus calculateStatus(LocalDateTime now) {
        return CouponStatus.of(this, now);
    }

    /**
     * 쿠폰이 발급 대기 상태인지 확인한다.
     */
    public boolean isReady(LocalDateTime now) {
        return now.isBefore(startAt);
    }

    /**
     * 쿠폰이 기간 만료 상태인지 확인한다.
     */
    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(endAt);
    }

    /**
     * 쿠폰 수량이 모두 소진되었는지 확인한다.
     */
    public boolean isExhausted() {
        return issuedQuantity >= totalQuantity;
    }

}
