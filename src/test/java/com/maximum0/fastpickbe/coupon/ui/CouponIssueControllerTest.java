package com.maximum0.fastpickbe.coupon.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
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
import com.maximum0.fastpickbe.common.security.principal.PrincipalDetails;
import com.maximum0.fastpickbe.coupon.application.CouponIssueService;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponIssueRequest;
import com.maximum0.fastpickbe.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

@DisplayName("쿠폰 발급 컨트롤러 단위 테스트")
class CouponIssueControllerTest extends BaseRestDocsTest {

    private final CouponIssueService couponIssueService = Mockito.mock(CouponIssueService.class);

    @Override
    protected Object initController() {
        return new CouponIssueController(couponIssueService);
    }

    @Nested
    @DisplayName("쿠폰 발급 요청 테스트")
    class IssueCouponApiTest {

        private final User testUser = User.forTest(1L, "test@test.com", "password", "테스터");
        private final PrincipalDetails principalDetails = new PrincipalDetails(testUser);

        @Test
        @DisplayName("정상적인 쿠폰 발급 요청 시 성공 응답을 반환한다")
        void issue_succeeds_withValidRequest() throws Exception {
            // given
            long couponId = 1L;
            long issuedCouponId = 100L;
            CouponIssueRequest request = new CouponIssueRequest(couponId);

            given(couponIssueService.issue(anyLong(), any(User.class))).willReturn(issuedCouponId);

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("S000"))
                    .andExpect(jsonPath("$.data").value(issuedCouponId))
                    .andDo(restDocument("coupon-issue/success",
                            requestFields(
                                    fieldWithPath("couponId").description("발급 요청할 쿠폰 ID")
                            ),
                            responseFields(successFields(
                                    fieldWithPath("data").description("생성된 발급 이력 ID")
                            ))
                    ));
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰을 요청하면 400 Bad Request 에러를 반환한다")
        void issue_returnsBadRequest_whenCouponAlreadyIssued() throws Exception {
            // given
            long couponId = 1L;
            CouponIssueRequest request = new CouponIssueRequest(couponId);
            ErrorCode errorCode = ErrorCode.ALREADY_ISSUED_COUPON;

            given(couponIssueService.issue(anyLong(), any(User.class)))
                    .willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
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
        @DisplayName("존재하지 않는 쿠폰 ID로 요청하면 404 Not Found 에러를 반환한다")
        void issue_returnsNotFound_whenCouponNotFound() throws Exception {
            // given
            long couponId = 999L;
            CouponIssueRequest request = new CouponIssueRequest(couponId);
            ErrorCode errorCode = ErrorCode.COUPON_NOT_FOUND;

            given(couponIssueService.issue(anyLong(), any(User.class)))
                    .willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andDo(restDocument("coupon-issue/fail-not-found",
                            responseFields(errorFields())
                    ));
        }
        @Test
        @DisplayName("쿠폰 수량이 소진된 경우 400 Bad Request 에러를 반환한다")
        void issue_returnsBadRequest_whenQuantityExhausted() throws Exception {
            // given
            long couponId = 1L;
            CouponIssueRequest request = new CouponIssueRequest(couponId);
            ErrorCode errorCode = ErrorCode.COUPON_EXHAUSTED;

            given(couponIssueService.issue(anyLong(), any(User.class)))
                    .willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(post("/api/v1/coupon-issues")
                            .with(user(principalDetails))
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
