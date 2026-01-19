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
import com.maximum0.fastpickbe.common.security.principal.PrincipalDetails;
import com.maximum0.fastpickbe.coupon.application.MyCouponService;
import com.maximum0.fastpickbe.coupon.domain.MyCouponStatus;
import com.maximum0.fastpickbe.coupon.ui.dto.MyCouponResponse;
import com.maximum0.fastpickbe.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("MyCouponController 단위 테스트")
class MyCouponControllerTest extends BaseRestDocsTest {

    private final MyCouponService myCouponService = Mockito.mock(MyCouponService.class);

    @Override
    protected Object initController() {
        return new MyCouponController(myCouponService);
    }

    @Nested
    @DisplayName("내 쿠폰 목록 조회 API 테스트")
    class GetMyCouponsApiTest {

        private final User testUser = User.forTest(1L, "user1@test.com", "pw", "유저1");
        private final PrincipalDetails principalDetails = new PrincipalDetails(testUser);

        @Test
        @DisplayName("내 쿠폰 목록을 필터링과 함께 성공적으로 조회한다")
        void getMyCoupons_succeeds_withValidRequest() throws Exception {
            // given
            MyCouponResponse response = new MyCouponResponse(
                    1L,
                    10L,
                    "할인쿠폰A",
                    LocalDateTime.now().plusDays(10),
                    MyCouponStatus.AVAILABLE
            );
            PageImpl<MyCouponResponse> pageResponse = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

            given(myCouponService.getMyCoupons(any(User.class), any(), any(Pageable.class)))
                    .willReturn(pageResponse);

            // when & then
            mockMvc.perform(getRequest("/api/v1/my/coupons")
                            .with(user(principalDetails))
                            .header("Authorization", "Bearer dummy-token")
                            .param("search", "할인")
                            .param("status", "AVAILABLE")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("S000"))
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
                                    fieldWithPath("data.content[].id").description("발급된 쿠폰의 고유 ID"),
                                    fieldWithPath("data.content[].couponId").description("원본 쿠폰의 ID"),
                                    fieldWithPath("data.content[].title").description("쿠폰 이름"),
                                    fieldWithPath("data.content[].expireAt").description("쿠폰 만료 일시"),
                                    fieldWithPath("data.content[].status").description("쿠폰 상태 (AVAILABLE, USED, EXPIRED)")
                            ))
                    ));
        }
    }
}
