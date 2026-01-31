package com.maximum0.fastpickbe.user.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 내 사용자의 권한 역할을 정의한다.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자")
    ;

    private final String key;
    private final String title;

    /**
     * Spring Security 권한 체계에 필요한 Prefix를 포함한 키 값을 반환한다.
     */
    public String getWithPrefix() {
        return key;
    }

}
