package com.maximum0.fastpickbe.common.security.principal;

import com.maximum0.fastpickbe.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * 사용자의 이메일을 기반으로 사용자 정보를 조회합니다.
     *
     * @param email 조회할 사용자의 이메일
     * @return 시큐리티 인증용 객체 (PrincipalDetails)
     * @throws UsernameNotFoundException 해당 이메일을 가진 사용자가 없을 경우 발생
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(PrincipalDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. email: " + email));
    }
}
