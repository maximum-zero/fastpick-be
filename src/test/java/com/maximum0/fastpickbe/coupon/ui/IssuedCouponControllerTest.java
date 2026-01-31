package com.maximum0.fastpickbe.coupon.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maximum0.fastpickbe.base.BaseRestDocsTest;
import com.maximum0.fastpickbe.common.dto.PageResponse;
import com.maximum0.fastpickbe.common.response.ApiResponse;
import com.maximum0.fastpickbe.common.security.principal.PrincipalDetails;
import com.maximum0.fastpickbe.coupon.application.service.IssuedCouponService;
import com.maximum0.fastpickbe.coupon.domain.vo.IssuedCouponStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.vo.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("Issued Coupon Controller 슬라이스 테스트")
class IssuedCouponControllerTest extends BaseRestDocsTest {

    private final IssuedCouponService issuedCouponService = Mockito.mock(IssuedCouponService.class);

    @Override
    protected Object initController() {
        return new IssuedCouponController(issuedCouponService);
    }

    @Nested
    @DisplayName("내 쿠폰 목록 조회 시나리오 테스트")
    class Get_My_Coupons_Scenario {

        private final User testUser = User.forTest(1L, "user1@test.com", "pw", "유저1", UserRole.USER);
        private final PrincipalDetails principalDetails = new PrincipalDetails(testUser);

        @Test
        @DisplayName("유효한 조회 요청 정보가 주어지면 내 쿠폰 목록을 조회하고 200 OK를 반환한다")
        void givenValidRequest_whenGetMyCoupons_thenReturnsMyCouponPage() throws Exception {
            // given
            MyCouponResponse response = createMyCouponResponse(1L, "할인쿠폰A");

            given(issuedCouponService.getIssuedCoupons(any(User.class), any(), any(Pageable.class)))
                    .willReturn(PageResponse.from(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)));

            // when & then
            mockMvc.perform(getRequest("/api/v1/my/coupons")
                            .with(user(principalDetails))
                            .header("Authorization", "Bearer dummy-token")
                            .param("search", "할인")
                            .param("status", "AVAILABLE")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ApiResponse.SUCCESS_CODE))
                    .andDo(restDocument("my-coupons/list",
                            requestHeaders(
                                    headerWithName("Authorization").description("Bearer <ACCESS_TOKEN>")
                            ),
                            queryParameters(
                                    parameterWithName("search").description("검색 키워드 (제목)").optional(),
                                    parameterWithName("status").description("쿠폰 상태 (ALL, AVAILABLE, EXPIRED)").optional(),
                                    parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size").description("페이지 사이즈").optional()
                            ),
                            responseFields(successPageFields(
                                    fieldWithPath("data.content[].id").description("발급 번호 ID"),
                                    fieldWithPath("data.content[].couponId").description("쿠폰 ID"),
                                    fieldWithPath("data.content[].brand").description("쿠폰 브랜드"),
                                    fieldWithPath("data.content[].title").description("쿠폰 이름"),
                                    fieldWithPath("data.content[].summary").description("쿠폰 요약 설명"),
                                    fieldWithPath("data.content[].totalQuantity").description("총 수량"),
                                    fieldWithPath("data.content[].issuedQuantity").description("현재 발급 수량"),
                                    fieldWithPath("data.content[].expireAt").description("쿠폰 만료 일시"),
                                    fieldWithPath("data.content[].status").description("쿠폰 상태 (AVAILABLE, USED, EXPIRED)")
                            ))
                    ));
        }
    }

    // --- 테스트 메서드 ---

    private MyCouponResponse createMyCouponResponse(Long id, String title) {
        return new MyCouponResponse(
                id, 10L, "29CM", title, "요약 설명",
                100, 10, LocalDateTime.now().plusDays(10),
                IssuedCouponStatus.AVAILABLE
        );
    }

}
