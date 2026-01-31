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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @param request 가입 요청 정보
     * @return 생성된 유저 정보와 발급된 인증 토큰(Access/Refresh) 응답 객체
     * @throws BusinessException 이메일이 이미 존재할 경우 (DUPLICATE_EMAIL)
     */
    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.name());
        User savedUser = userRepository.save(user);

        return createAuthResponse(savedUser);
    }

    /**
     * 유저 로그인을 처리하여 인증 권한을 부여한다.
     * 이메일 존재 여부 및 비밀번호 일치 여부를 검증한다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 인증된 유저 정보와 발급된 인증 토큰(Access/Refresh) 응답 객체
     * @throws BusinessException 아이디가 없거나 비밀번호가 틀린 경우 (LOGIN_FAILED)
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        user.authenticate(passwordEncoder, request.password());

        return createAuthResponse(user);
    }

    /**
     * 특정 사용자에 대한 인증 객체를 생성하고 JWT 토큰 응답을 빌드한다.
     *
     * @param user 인증 대상 유저 엔티티
     * @return 토큰 정보를 포함한 AuthResponse 객체
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
     * 특정 사용자에 대한 인증 객체를 생성하고 JWT 토큰 응답을 빌드한다.
     *
     * @param user 인증 대상 유저 엔티티
     * @return 토큰 정보(Access/Refresh)를 포함한 AuthResponse 객체
     */
    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getWithPrefix()))
        );
    }

}
