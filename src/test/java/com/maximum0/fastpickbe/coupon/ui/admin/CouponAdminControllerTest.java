package com.maximum0.fastpickbe.coupon.ui.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maximum0.fastpickbe.base.BaseRestDocsTest;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.admin.CouponAdminService;
import com.maximum0.fastpickbe.coupon.ui.admin.dto.CouponCreateRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("쿠폰 관리자 컨트롤러 단위 테스트")
class CouponAdminControllerTest extends BaseRestDocsTest {
    private final CouponAdminService couponAdminService = Mockito.mock(CouponAdminService.class);

    @Override
    protected Object initController() {
        return new CouponAdminController(couponAdminService);
    }

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Nested
    @DisplayName("쿠폰 생성 테스트")
    class CreateCouponTest {

        @Test
        @DisplayName("올바른 쿠폰 정보를 입력하면 쿠폰을 생성하고 Created를 반환한다.")
        void createCoupon_returnsCreated_whenRequestIsValid() throws Exception {
            // given
            Long savedId = 1L;
            CouponCreateRequest request = new CouponCreateRequest(
                    "나이키",
                    "[특가] 에어포스 1",
                    "요약 설명",
                    "상세 설명입니다.",
                    100,
                    now.plusDays(1),
                    now.plusDays(7)
            );

            given(couponAdminService.createCoupon(any())).willReturn(savedId);

            // when & then
            mockMvc.perform(postRequest("/api/v1/admin/coupons", request))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/admin/coupons/" + savedId))
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andExpect(jsonPath("$.data").value(savedId))
                    .andDo(restDocument("admin/coupon/create",
                            requestFields(
                                    fieldWithPath("brand").description("브랜드명"),
                                    fieldWithPath("title").description("쿠폰 제목"),
                                    fieldWithPath("summary").description("요약 설명"),
                                    fieldWithPath("description").description("상세 설명"),
                                    fieldWithPath("totalQuantity").description("총 발행 수량"),
                                    fieldWithPath("startAt").description("발급 시작 일시 (yyyy-MM-dd'T'HH:mm:ss)"),
                                    fieldWithPath("endAt").description("발급 종료 일시 (yyyy-MM-dd'T'HH:mm:ss)")
                            ),
                            responseHeaders(
                                    headerWithName("Location").description("생성된 쿠폰의 상세 조회 URI")
                            ),
                            responseFields(successFields(
                                    fieldWithPath("data").description("생성된 쿠폰 식별자")
                            ))
                    ));
        }

        @Test
        @DisplayName("필수값이 누락되면 INVALID_INPUT_VALUE 예외를 반환한다.")
        void createCoupon_returnsBadRequest_whenRequestIsInvalid() throws Exception {
            // given
            CouponCreateRequest invalidRequest = new CouponCreateRequest("", "", "", "", 0, null, null);
            ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

            // when & then
            mockMvc.perform(postRequest("/api/v1/admin/coupons", invalidRequest))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()))
                    .andDo(restDocument("admin/coupon/create-fail",
                            responseFields(errorFields())
                    ));
        }
    }
}