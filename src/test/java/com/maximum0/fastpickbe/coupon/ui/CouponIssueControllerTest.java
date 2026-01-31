package com.maximum0.fastpickbe.coupon.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maximum0.fastpickbe.base.BaseRestDocsTest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.common.security.principal.PrincipalDetails;
import com.maximum0.fastpickbe.coupon.application.service.CouponIssueService;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponIssueRequest;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

@DisplayName("Coupon Issue Controller 슬라이스 테스트")
class CouponIssueControllerTest extends BaseRestDocsTest {

    private final CouponIssueService couponIssueService = Mockito.mock(CouponIssueService.class);

    @Override
    protected Object initController() {
        return new CouponIssueController(couponIssueService);
    }

    @Nested
    @DisplayName("쿠폰 발급 요청 시나리오 테스트")
    class Issue_Coupon_Scenario {

        private final User testUser = User.forTest(1L, "test@test.com", "password", "테스터", UserRole.USER);
        private final PrincipalDetails principalDetails = new PrincipalDetails(testUser);

        @Test
        @DisplayName("정상적인 쿠폰 발급 요청 정보가 주어지면 발급을 수행하고 200 OK를 반환한다")
        void givenValidRequest_whenIssueCoupon_thenReturnsIssuedId() throws Exception {
            // given
            long couponId = 1L;
            long issuedCouponId = 100L;
            CouponIssueRequest request = new CouponIssueRequest(couponId);

            given(couponIssueService.issue(anyLong(), any(User.class))).willReturn(issuedCouponId);

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
                            .header("Authorization", "Bearer dummy-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data").value(issuedCouponId))
                    .andDo(restDocument("coupon-issue/success",
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer <ACCESS_TOKEN>")
                            ),
                            requestFields(
                                    fieldWithPath("couponId").description("발급 요청할 쿠폰 ID")
                            ),
                            responseFields(successFields(
                                    fieldWithPath("data").description("생성된 발급 이력 ID")
                            ))
                    ));
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰 정보가 주어지면 쿠폰 중복 발급 실패 예외를 반환한다")
        void givenAlreadyIssuedCoupon_whenIssueCoupon_thenThrowsExceptionByAlreadyIssued() throws Exception {
            // given
            CouponIssueRequest request = new CouponIssueRequest(1L);
            ErrorCode errorCode = ErrorCode.ALREADY_ISSUED_COUPON;

            given(couponIssueService.issue(anyLong(), any(User.class)))
                    .willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
                            .header("Authorization", "Bearer dummy-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()))
                    .andDo(restDocument("coupon-issue/fail-already-issued",
                            responseFields(errorFields())
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID가 주어지면 쿠폰 조회 실패 예외를 반환한다")
        void givenNonExistentId_whenIssueCoupon_thenThrowsExceptionByCouponNotFound() throws Exception {
            // given
            CouponIssueRequest request = new CouponIssueRequest(999L);
            ErrorCode errorCode = ErrorCode.COUPON_NOT_FOUND;

            given(couponIssueService.issue(anyLong(), any(User.class)))
                    .willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
                            .header("Authorization", "Bearer dummy-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andDo(restDocument("coupon-issue/fail-not-found",
                            responseFields(errorFields())
                    ));
        }

        @Test
        @DisplayName("재고가 소진된 쿠폰 정보가 주어지면 쿠폰 수량 소진 예외를 반환한다")
        void givenExhaustedQuantity_whenIssueCoupon_thenThrowsExceptionByQuantityExhausted() throws Exception {
            // given
            CouponIssueRequest request = new CouponIssueRequest(1L);
            ErrorCode errorCode = ErrorCode.COUPON_EXHAUSTED;

            given(couponIssueService.issue(anyLong(), any(User.class)))
                    .willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
                            .header("Authorization", "Bearer dummy-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andDo(restDocument("coupon-issue/fail-exhausted",
                            responseFields(errorFields())
                    ));
        }
    }

}
