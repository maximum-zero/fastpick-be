package com.maximum0.fastpickbe.coupon.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maximum0.fastpickbe.base.BaseRestDocsTest;
import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.coupon.application.CouponService;
import com.maximum0.fastpickbe.coupon.domain.CouponStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponResponse;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("쿠폰 컨트롤러 단위 테스트")
class CouponControllerTest extends BaseRestDocsTest {
    private final CouponService couponService = Mockito.mock(CouponService.class);

    @Override
    protected Object initController() {
        return new CouponController(couponService);
    }

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 1, 0);

    @Nested
    @DisplayName("쿠폰 목록 조회 테스트")
    class GetCouponsTest {

        @Test
        @DisplayName("검색 조건과 페이지 번호로 요청하면 쿠폰 목록을 반환한다.")
        void getCoupons_returnsPage_whenRequestIsValid() throws Exception {
            // given
            CouponSummaryResponse summary = CouponSummaryResponse.builder()
                    .id(1L)
                    .title("할인 쿠폰")
                    .startAt(now.minusDays(1))
                    .endAt(now.plusDays(1))
                    .status(CouponStatus.ISSUING.name())
                    .build();

            given(couponService.getCoupons(any(), any()))
                    .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));

            // when & then
            mockMvc.perform(getRequest("/api/v1/coupons")
                            .param("search", "할인")
                            .param("filterType", "ISSUING")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andDo(restDocument("coupon/list",
                            queryParameters(
                                    parameterWithName("search").description("검색 키워드 (제목)").optional(),
                                    parameterWithName("filterType").description("필터 타입 (ALL, READY, ISSUING, CLOSED)").optional(),
                                    parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size").description("페이지 사이즈").optional()
                            ),
                            responseFields(successPageFields(
                                    fieldWithPath("data.content[].id").description("쿠폰 식별자"),
                                    fieldWithPath("data.content[].brand").description("쿠폰 브랜드"),
                                    fieldWithPath("data.content[].title").description("쿠폰 제목"),
                                    fieldWithPath("data.content[].summary").description("쿠폰 요약 설명"),
                                    fieldWithPath("data.content[].totalQuantity").description("총 수량"),
                                    fieldWithPath("data.content[].issuedQuantity").description("현재 발급 수량"),
                                    fieldWithPath("data.content[].startAt").description("발급 시작 일시"),
                                    fieldWithPath("data.content[].endAt").description("발급 종료 일시"),
                                    fieldWithPath("data.content[].status").description("현재 상태")
                            ))
                    ));
        }
    }

    @Nested
    @DisplayName("쿠폰 상세 조회 테스트")
    class GetCouponTest {

        @Test
        @DisplayName("존재하는 쿠폰 ID로 조회하면 상세 정보를 반환한다.")
        void getCoupon_returnsCouponResponse_whenIdExists() throws Exception {
            // given
            Long couponId = 1L;
            CouponResponse response = new CouponResponse(
                    couponId, "브랜드명", "할인 쿠폰", "요약 설명", 100, 0, now.minusDays(1), now.plusDays(1), CouponStatus.ISSUING
            );
            given(couponService.getCoupon(couponId)).willReturn(response);

            // when & then
            mockMvc.perform(getRequest("/api/v1/coupons/{id}", couponId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andDo(restDocument("coupon/detail",
                            pathParameters(
                                    parameterWithName("id").description("쿠폰 식별자")
                            ),
                            responseFields(successFields(
                                    fieldWithPath("data.id").description("쿠폰 식별자"),
                                    fieldWithPath("data.brand").description("쿠폰 브랜드"),
                                    fieldWithPath("data.title").description("쿠폰 제목"),
                                    fieldWithPath("data.description").description("쿠폰 설명"),
                                    fieldWithPath("data.totalQuantity").description("총 수량"),
                                    fieldWithPath("data.issuedQuantity").description("현재 발급 수량"),
                                    fieldWithPath("data.startAt").description("발급 시작 일시"),
                                    fieldWithPath("data.endAt").description("발급 종료 일시"),
                                    fieldWithPath("data.status").description("현재 상태")
                            ))
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 조회하면 COUPON_NOT_FOUND 예외를 반환한다.")
        void getCoupon_returnsNotFound_whenIdDoesNotExist() throws Exception {
            // given
            Long couponId = 999L;
            ErrorCode errorCode = ErrorCode.COUPON_NOT_FOUND;
            given(couponService.getCoupon(couponId)).willThrow(new BusinessException(errorCode));

            // when & then
            mockMvc.perform(getRequest("/api/v1/coupons/{id}", couponId))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()))
                    .andDo(restDocument("coupon/detail-fail",
                            responseFields(errorFields())
                    ));
        }
    }
}