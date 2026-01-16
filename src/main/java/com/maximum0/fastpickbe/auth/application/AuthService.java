package com.maximum0.fastpickbe.auth.application;

import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.auth.ui.dto.TokenResponse;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.security.provider.JwtTokenProvider;
import com.maximum0.fastpickbe.user.domain.User;
import com.maximum0.fastpickbe.user.domain.UserRepository;
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
     * 신규 유저 회원가입을 처리한다.
     * 이메일 중복 여부를 확인한 후, 유저 생성하여 저장한다.
     *
     * @param request 가입 요청 정보
     * @return 생성된 유저의 식별자(ID)
     * @throws BusinessException 이메일이 중복될 경우 (DUPLICATE_EMAIL)
     */
    @Transactional
    public Long signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.name());

        return userRepository.save(user).getId();
    }

    /**
     * 유저 로그인을 처리한다.
     *
     * @param request 로그인 요청 정보
     * @return AccessToken과 RefreshToken을 포함한 토큰 응답
     * @throws BusinessException 아이디 또는 비밀번호가 일치하지 않을 경우 (LOGIN_FAILED)
     */
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        user.authenticate(passwordEncoder, request.password());
        Authentication authentication = createAuthentication(user);

        return new TokenResponse(
                jwtTokenProvider.createAccessToken(authentication),
                jwtTokenProvider.createRefreshToken(authentication),
                "Bearer"
        );
    }

    /**
     * 유저 정보 기반의 시큐리티 인증 객체를 생성한다.
     */
    private Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getWithPrefix()))
        );
    }
}
