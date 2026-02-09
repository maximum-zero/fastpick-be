package com.maximum0.fastpickbe.auth.support;

import com.maximum0.fastpickbe.base.BaseIntegrationTest;
import com.maximum0.fastpickbe.common.security.provider.JwtTokenProvider;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@Disabled("k6 부하 테스트를 위한 사전 유저 토큰 생성 도구")
@DisplayName("User Token 생성 도구")
class TokenExtractor extends BaseIntegrationTest {

    @Autowired
    private
    JwtTokenProvider jwtProvider;

    @Test
    void extract_tokens_to_csv() throws IOException {
        FileWriter writer = new FileWriter("users.csv");
        writer.write("username,token\n");

        // When: 유저별로 토큰 생성해서 파일에 기입
        for (int i = 1; i <= 1000; i++) {
            String email = "user" + i + "@gmail.com";

            Authentication auth = new UsernamePasswordAuthenticationToken(email, null,
                    Collections.singleton(new SimpleGrantedAuthority(UserRole.USER.getWithPrefix())));
            String token = jwtProvider.createAccessToken(auth);
            writer.write(email + "," + token + "\n");
        }

        // Then: 파일 저장 완료
        writer.close();
        System.out.println("토큰 1,000개 추출 완료! users.csv 확인하세요!");
    }
}