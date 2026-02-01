package com.maximum0.fastpickbe.auth.application.service;

import com.maximum0.fastpickbe.auth.ui.dto.AuthResponse;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.security.provider.JwtTokenProvider;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 인증 관련 비즈니스 로직을 담당하는 서비스.
 *
 * - 신규 유저 회원가입
 * - 유저 로그인
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 신규 유저 회원가입을 처리하고 즉시 인증 권한을 부여한다.
     * 이메일 중복 여부를 확인한 후, 유저 정보를 암호화하여 저장한다.
     *
     * @throws BusinessException 이메일이 이미 존재할 경우 (DUPLICATE_EMAIL)
     */
    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.info("✅ [AuthService] 회원가입 실패 - 중복 이메일 | email: {}", request.email());
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.name());
        User savedUser = userRepository.save(user);

        log.info("✅ [AuthService] 신규 회원가입 완료 | userId: {}, email: {}", savedUser.getId(), savedUser.getEmail());
        return createAuthResponse(savedUser);
    }

    /**
     * 유저 로그인을 처리하여 인증 권한을 부여한다.
     * 이메일 존재 여부 및 비밀번호 일치 여부를 검증한다.
     *
     * @throws BusinessException 아이디가 없거나 비밀번호가 틀린 경우 (LOGIN_FAILED)
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.info("✅ [AuthService] 로그인 실패 - 존재하지 않는 이메일 | email: {}", request.email());
                    return new BusinessException(ErrorCode.LOGIN_FAILED);
                });

        user.authenticate(passwordEncoder, request.password());

        return createAuthResponse(user);
    }

    /**
     * 인증 객체를 기반으로 Access / Refresh Token을 생성하여 응답 객체를 구성한다.
     */
    private AuthResponse createAuthResponse(User user) {
        Authentication authentication = createAuthentication(user);

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                "Bearer",
                user
        );
    }

    /**
     * 인증 객체(Authentication)를 생성한다.
     * JWT 토큰 발급에 사용된다.
     */
    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getWithPrefix()))
        );
    }

}
