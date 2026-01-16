package com.maximum0.fastpickbe.auth.ui.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String grantType
) {

}
