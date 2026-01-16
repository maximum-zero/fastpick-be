package com.maximum0.fastpickbe.auth.ui;

import com.maximum0.fastpickbe.auth.application.AuthService;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.auth.ui.dto.TokenResponse;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 신규 회원가입을 처리한다.
     *
     * @param request 가입 정보
     * @return 생성된 유저의 식별자(ID)를 담은 공통 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.signUp(request)));
    }

    /**
     * 로그인을 처리한다.
     *
     * @param request 로그인 정보
     * @return 인증된 유저의 식별자(ID)를 담은 공통 응답
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

}
