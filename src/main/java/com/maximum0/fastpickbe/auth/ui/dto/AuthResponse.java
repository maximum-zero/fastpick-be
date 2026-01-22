package com.maximum0.fastpickbe.auth.ui.dto;

import com.maximum0.fastpickbe.user.domain.User;
import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String grantType,
        UserInfo user
) {
    public record UserInfo(
            Long id,
            String email,
            String name
    ) {}

    public static AuthResponse of(String accessToken, String refreshToken, String grantType, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .grantType(grantType)
                .user(new UserInfo(user.getId(), user.getEmail(), user.getName()))
                .build();
    }
}