package com.maximum0.fastpickbe.auth.application;

import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.user.domain.User;
import com.maximum0.fastpickbe.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;

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

        User user = User.create(request.email(), request.password(), request.name());
        return userRepository.save(user).getId();
    }

    /**
     * 유저 로그인을 처리한다.
     *
     * @param request 로그인 요청 정보
     * @return 인증에 성공한 유저의 식별자(ID)
     * @throws BusinessException 아이디 또는 비밀번호가 일치하지 않을 경우 (LOGIN_FAILED)
     */
    public Long login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!user.getPassword().equals(request.password())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return user.getId();
    }
}
