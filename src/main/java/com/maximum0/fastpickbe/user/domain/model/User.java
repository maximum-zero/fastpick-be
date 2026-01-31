package com.maximum0.fastpickbe.user.domain.model;

import com.maximum0.fastpickbe.common.domain.BaseEntity;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // --- 생성자 ---

    @Builder(access = AccessLevel.PRIVATE)
    private User(Long id, String email, String password, String name, UserRole role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    // --- 정적 팩토리 메서드 ---

    /**
     * 유저 객체를 생성한다. (비즈니스 로직)
     */
    public static User create(String email, String rawPassword, String name) {
        return User.builder()
                .email(email)
                .password(rawPassword)
                .name(name)
                .role(UserRole.USER)
                .build();
    }

    /**
     * 유저 객체를 생성한다. (테스트 코드)
     */
    public static User forTest(Long id, String email, String rawPassword, String name, UserRole userRole) {
        return User.builder()
                .id(id)
                .email(email)
                .password(rawPassword)
                .name(name)
                .role(userRole)
                .build();
    }

    // --- 비즈니스 행위 로직 ---

    /**
     * 입력받은 비밀번호의 유효성을 검증한다.
     * @param passwordEncoder 암호화 인코더
     * @param rawPassword 검증할 평문 비밀번호
     * @throws BusinessException 비밀번호가 일치하지 않을 경우
     */
    public void authenticate(PasswordEncoder passwordEncoder, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, this.password)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
    }

}
