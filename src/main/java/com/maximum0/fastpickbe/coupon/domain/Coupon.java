package com.maximum0.fastpickbe.coupon.domain;

import com.maximum0.fastpickbe.common.domain.BaseEntity;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
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

@Getter
@Entity
@Table(name = "tb_coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponUseStatus useStatus;

    @Builder(access = AccessLevel.PRIVATE)
    public Coupon(Long id, String title, int totalQuantity, int issuedQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        this.id = id;
        this.title = title;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.useStatus = CouponUseStatus.AVAILABLE;
    }

    // --- 정적 팩토리 메서드 ---
    public static Coupon create(String title, int totalQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.builder()
                .title(title)
                .totalQuantity(totalQuantity)
                .issuedQuantity(0)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    public static Coupon create(String title, int totalQuantity, int issuedQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.builder()
                .title(title)
                .totalQuantity(totalQuantity)
                .issuedQuantity(issuedQuantity)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    public static Coupon forTest(Long id, String title, int totalQuantity, int issuedQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.builder()
                .id(id)
                .title(title)
                .totalQuantity(totalQuantity)
                .issuedQuantity(issuedQuantity)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    // --- 상태 확인 및 계산 로직 ---

    /**
     * 쿠폰이 발행 대기 상태(시작 전)인지 확인합니다.
     * @param now 기준 시간
     * @return 현재 시각 < 시작 시각이면 true
     */
    public boolean isReady(LocalDateTime now) {
        return now.isBefore(startAt);
    }

    /**
     * 쿠폰이 발행 만료 상태인지 확인합니다.
     * @param now 기준 시간
     * @return 현재 시각 >= 종료 시각이면 true
     */
    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(endAt);
    }

    /**
     * 쿠폰이 소진(품절) 상태인지 확인합니다.
     * @return 발행 수량 >= 전체 수량이면 true
     */
    public boolean isExhausted() {
        return issuedQuantity >= totalQuantity;
    }

    /**
     * 현재 쿠폰의 상태를 동적으로 계산하여 반환합니다.
     * @param now 기준 시간
     * @return 쿠폰의 현재 상태 (READY, ISSUING, EXHAUSTED, EXPIRED, DISABLED)
     */
    public CouponStatus calculateStatus(LocalDateTime now) {
        if (this.useStatus == CouponUseStatus.DISABLED) return CouponStatus.DISABLED;
        if (isReady(now)) return CouponStatus.READY;
        if (isExpired(now)) return CouponStatus.EXPIRED;
        if (isExhausted()) return CouponStatus.EXHAUSTED;
        return CouponStatus.ISSUING;
    }

    // --- 비즈니스 행위 로직 ---

    /**
     * 쿠폰 발행 조건을 검증합니다.
     * @param now 기준 시간
     * @throws BusinessException 조건 위반 시 적절한 에러 발생
     */
    public void validateIssuanceCondition(LocalDateTime now) {
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
     * 쿠폰을 발급합니다.
     * 발급 조건을 검증한 후, 발급된 쿠폰 수량을 1 증가시킵니다.
     * @param now 기준 시간
     */
    public void issue(LocalDateTime now) {
        validateIssuanceCondition(now);
        this.issuedQuantity++;
    }

    /**
     * 쿠폰을 중단(비활성화) 처리합니다.
     * 관리자에 의해 쿠폰 발급 및 조회를 제한할 때 사용합니다.
     */
    public void disable() {
        this.useStatus = CouponUseStatus.DISABLED;
    }

}
