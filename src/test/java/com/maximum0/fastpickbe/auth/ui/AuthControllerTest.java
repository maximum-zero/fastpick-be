package com.maximum0.fastpickbe.auth.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maximum0.fastpickbe.auth.application.AuthService;
import com.maximum0.fastpickbe.auth.ui.dto.AuthResponse;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.base.BaseRestDocsTest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("AuthController 단위 테스트")
class AuthControllerTest extends BaseRestDocsTest {

    private final AuthService authService = Mockito.mock(AuthService.class);

    @Override
    protected Object initController() {
        return new AuthController(authService);
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignUpTest {

        @Test
        @DisplayName("올바른 가입 정보로 요청하면 성공 코드와 생성된 ID를 반환한다.")
        void signUp_returnsUserId_whenRequestIsValid() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("test@test.com", "password123", "테스터");

            User newUser = User.forTest(1L, "test@test.com", "password123", "테스터");
            String accessToken = "access-token";
            String refreshToken = "refresh-token";
            String grantType = "Bearer";

            AuthResponse authResponse = AuthResponse.of(accessToken, refreshToken, grantType, newUser);
            given(authService.signUp(any())).willReturn(authResponse);

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/signup", request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data.accessToken").value(accessToken))
                    .andExpect(jsonPath("$.data.refreshToken").value(refreshToken))
                    .andExpect(jsonPath("$.data.grantType").value(grantType))
                    .andExpect(jsonPath("$.data.user.id").value(newUser.getId()))
                    .andExpect(jsonPath("$.data.user.email").value(newUser.getEmail()))
                    .andExpect(jsonPath("$.data.user.name").value(newUser.getName()))
                    .andDo(restDocument("auth/signup",
                            requestFields(
                                    fieldWithPath("email").description("이메일"),
                                    fieldWithPath("password").description("비밀번호 (8자 이상)"),
                                    fieldWithPath("name").description("이름 (2~10자)")
                            ),
                            responseFields(successFields(
                                    fieldWithPath("data.accessToken").description("Access 토큰"),
                                    fieldWithPath("data.refreshToken").description("Refresh 토큰"),
                                    fieldWithPath("data.grantType").description("인증 타입"),
                                    fieldWithPath("data.user.id").description("유저 식별자"),
                                    fieldWithPath("data.user.email").description("유저 이메일"),
                                    fieldWithPath("data.user.name").description("유저 이름")
                            ))
                    ));
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 INVALID_INPUT_VALUE 예외를 반환한다.")
        void signUp_returnsBadRequest_whenEmailIsInvalid() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("invalid-email", "password123", "테스터");

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/signup", request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                    .andDo(restDocument("auth/signup-fail",
                            responseFields(errorFields())
                    ));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외를 반환한다.")
        void signUp_returnsConflict_whenEmailAlreadyExists() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("exists@test.com", "password123", "테스터");
            ErrorCode errorCode = ErrorCode.DUPLICATE_EMAIL;
            given(authService.signUp(any())).willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/signup", request))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()))
                    .andDo(restDocument("auth/signup-fail-duplicate",
                            responseFields(errorFields())
                    ));
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 성공 코드와 토큰을 반환한다.")
        void login_returnsTokenResponse_whenCredentialsAreValid() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User user = User.forTest(1L, "test@test.com", "password123", "테스터");
            String accessToken = "access-token";
            String refreshToken = "refresh-token";
            String grantType = "Bearer";

            AuthResponse authResponse = AuthResponse.of(accessToken, refreshToken, grantType, user);
            given(authService.login(any())).willReturn(authResponse);

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/login", request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data.accessToken").value(accessToken))
                    .andExpect(jsonPath("$.data.refreshToken").value(refreshToken))
                    .andExpect(jsonPath("$.data.grantType").value(grantType))
                    .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                    .andExpect(jsonPath("$.data.user.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.data.user.name").value(user.getName()))
                    .andDo(restDocument("auth/login",
                            requestFields(
                                    fieldWithPath("email").description("이메일"),
                                    fieldWithPath("password").description("비밀번호 (8자 이상)")
                            ),
                            responseFields(successFields(
                                    fieldWithPath("data.accessToken").description("Access 토큰"),
                                    fieldWithPath("data.refreshToken").description("Refresh 토큰"),
                                    fieldWithPath("data.grantType").description("인증 타입"),
                                    fieldWithPath("data.user.id").description("유저 식별자"),
                                    fieldWithPath("data.user.email").description("유저 이메일"),
                                    fieldWithPath("data.user.name").description("유저 이름")
                            ))
                    ));
        }

        @Test
        @DisplayName("로그인 정보가 일치하지 않으면 LOGIN_FAILED 예외를 반환한다.")
        void login_returnsUnauthorized_whenLoginFails() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrong-password");
            ErrorCode errorCode = ErrorCode.LOGIN_FAILED;
            given(authService.login(any())).willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/login", request))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()))
                    .andDo(restDocument("auth/login-fail",
                            responseFields(errorFields())
                    ));
        }
    }
}
