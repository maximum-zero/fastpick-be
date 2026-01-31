package com.maximum0.fastpickbe.auth.domain.model;

import com.maximum0.fastpickbe.common.domain.BaseEntity;
import com.maximum0.fastpickbe.user.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_refresh_token")
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryAt;

    // --- 생성자 ---

    @Builder(access = AccessLevel.PRIVATE)
    private RefreshToken(Long id, String token, User user, LocalDateTime expiryAt) {
        this.id = id;
        this.token = token;
        this.user = user;
        this.expiryAt = expiryAt;
    }

    // --- 정적 팩토리 메서드 ---

    /**
     * 리프레시 토큰 객체를 생성한다. (비즈니스)
     */
    public static RefreshToken create(String token, User user, LocalDateTime expiryAt) {
        return RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryAt(expiryAt)
                .build();
    }

    /**
     * 리프레시 토큰 객체를 생성한다. (테스트 코드)
     */
    public static RefreshToken forTest(Long id, String token, User user, LocalDateTime expiryAt) {
        return RefreshToken.builder()
                .id(id)
                .token(token)
                .user(user)
                .expiryAt(expiryAt)
                .build();
    }

    // --- 비즈니스 행위 로직 ---

    /**
     * 리프레시 토큰 정보를 갱신한다.
     * @param newToken 새로운 토큰 문자열
     * @param newExpiryAt 새로운 만료 시각
     */
    public void update(String newToken, LocalDateTime newExpiryAt) {
        this.token = newToken;
        this.expiryAt = newExpiryAt;
    }

    // --- 상태 판별 및 계산 로직 ---

    /**
     * 토큰이 만료되었는지 확인한다.
     * @param now 기준 시각
     * @return 만료 여부
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiryAt);
    }

}
