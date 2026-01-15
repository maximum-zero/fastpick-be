package com.maximum0.fastpickbe.auth.domain;

import com.maximum0.fastpickbe.common.domain.BaseEntity;
import com.maximum0.fastpickbe.user.domain.User;
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

@Getter
@Entity
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

    @Builder(access = AccessLevel.PRIVATE)
    private RefreshToken(String token, User user, LocalDateTime expiryAt) {
        this.token = token;
        this.user = user;
        this.expiryAt = expiryAt;
    }

    public static RefreshToken create(String token, User user, LocalDateTime expiryAt) {
        return RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryAt(expiryAt)
                .build();
    }

    // --- 비즈니스 로직 ---

    /**
     * 리프레시 토큰의 정보를 업데이트합니다.
     * @param newToken 새로운 토큰 문자열
     * @param newExpiryAt 새로운 만료 시간
     */
    public void update(String newToken, LocalDateTime newExpiryAt) {
        this.token = newToken;
        this.expiryAt = newExpiryAt;
    }

    /**
     * 리프레시 토큰이 만료되었는지 확인합니다.
     * @param now 기준 시간
     * @return 현재 시각 > 만료 시각이면 true
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiryAt);
    }

}
