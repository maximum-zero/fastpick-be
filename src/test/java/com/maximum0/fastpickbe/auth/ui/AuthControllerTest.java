package com.maximum0.fastpickbe.auth.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maximum0.fastpickbe.auth.application.service.AuthService;
import com.maximum0.fastpickbe.auth.ui.dto.AuthResponse;
import com.maximum0.fastpickbe.auth.ui.dto.LoginRequest;
import com.maximum0.fastpickbe.auth.ui.dto.SignUpRequest;
import com.maximum0.fastpickbe.base.BaseRestDocsTest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Auth Controller 슬라이스 테스트")
class AuthControllerTest extends BaseRestDocsTest {

    private final AuthService authService = Mockito.mock(AuthService.class);

    @Override
    protected Object initController() {
        return new AuthController(authService);
    }

    @Nested
    @DisplayName("회원가입 시나리오 테스트")
    class Sign_Up_Scenario {

        @Test
        @DisplayName("유효한 가입 정보가 주어지고 가입을 요청하면 성공 코드와 토큰 정보를 반환한다")
        void givenValidRequest_whenSignUp_thenReturnsAuthResponse() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("test@test.com", "password123", "테스터");
            User user = User.forTest(1L, "test@test.com", "password123", "테스터", UserRole.USER);
            AuthResponse authResponse = createAuthResponse(user);

            given(authService.signUp(any())).willReturn(authResponse);

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/signup", request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"))
                    .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                    .andExpect(jsonPath("$.data.user.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.data.user.name").value(user.getName()))
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
        @DisplayName("이메일 형식이 올바르지 않은 정보가 주어지면 입력값 유효성 예외를 반환한다")
        void givenInvalidEmail_whenSignUp_thenThrowsExceptionByInvalidInput() throws Exception {
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
        @DisplayName("이미 사용 중인 이메일 정보가 주어지면 중복 이메일 예외를 반환한다")
        void givenDuplicateEmail_whenSignUp_thenThrowsExceptionByDuplicateEmail() throws Exception {
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
    @DisplayName("로그인 시나리오 테스트")
    class Login_Scenario {

        @Test
        @DisplayName("유효한 로그인 정보가 주어지고 로그인을 요청하면 성공 코드와 토큰 정보를 반환한다")
        void givenValidCredentials_whenLogin_thenReturnsAuthResponse() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User user = User.forTest(1L, "test@test.com", "password123", "테스터", UserRole.USER);
            AuthResponse authResponse = createAuthResponse(user);

            given(authService.login(any())).willReturn(authResponse);

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/login", request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"))
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
        @DisplayName("잘못된 로그인 정보가 주어지면 로그인 실패 예외를 반환한다")
        void givenInvalidCredentials_whenLogin_thenThrowsExceptionByLoginFailed() throws Exception {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrong-password");
            ErrorCode errorCode = ErrorCode.LOGIN_FAILED;
            given(authService.login(any())).willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(postRequest("/api/v1/auth/login", request))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andDo(restDocument("auth/login-fail",
                            responseFields(errorFields())
                    ));
        }
    }

    // --- 테스트 메서드 ---

    private AuthResponse createAuthResponse(User user) {
        return AuthResponse.of("access-token", "refresh-token", "Bearer", user);
    }

}
