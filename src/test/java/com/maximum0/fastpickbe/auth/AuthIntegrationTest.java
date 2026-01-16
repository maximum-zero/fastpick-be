package com.maximum0.fastpickbe.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("인증 도메인 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("회원가입과 로그인 연계 테스트")
    class SignUpAndLoginTest {

        @Test
        @DisplayName("정상적인 회원가입 후, 해당 정보로 로그인을 수행하면 성공한다.")
        void signUpAndLogin_Success_WhenCredentialsAreValid() throws Exception {
            // given: 회원가입 정보
            String email = "testuser@example.com";
            String password = "password123!";
            String username = "테스트유저";
            SignUpRequest signUpRequest = new SignUpRequest(email, password, username);

            // when: 회원가입 요청
            ResultActions signUpActions = mockMvc.perform(post("/api/v1/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)));

            // then: 회원가입 성공 검증
            signUpActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data").exists());

            // given: 로그인 정보
            LoginRequest loginRequest = new LoginRequest(email, password);

            // when: 로그인 요청
            ResultActions loginActions = mockMvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // then: 로그인 성공 검증
            loginActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));
        }
    }
}
