package com.maximum0.fastpickbe.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.base.BaseIntegrationTest;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("Auth Domain 통합 테스트")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("회원가입 및 로그인 연계 시나리오 테스트")
    class Auth_Flow_Scenario {

        @Test
        @DisplayName("유효한 가입 정보로 회원가입 후 로그인을 수행하면 인증 토큰을 반환한다")
        void givenValidCredentials_whenSignUpAndLogin_thenReturnsAuthTokens() throws Exception {
            // given: 회원가입 단계
            String email = "testuser@example.com";
            String password = "password123!";
            SignUpRequest signUpRequest = new SignUpRequest(email, password, "테스트유저");

            // when: 회원가입 요청
            mockMvc.perform(post("/api/v1/auth/signup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE));

            // given: 로그인 단계
            LoginRequest loginRequest = new LoginRequest(email, password);

            // when: 로그인 요청
            ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // then: 최종 인증 정보 검증
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));
        }
    }

}
