package com.maximum0.fastpickbe.coupon.application.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.maximum0.fastpickbe.coupon.application.service.CouponKeywordManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Coupon Keyword Manager 단위 테스트")
class CouponKeywordManagerTest {
    private final CouponKeywordManager couponKeywordManager = new CouponKeywordManager();

    @Nested
    @DisplayName("키워드 추출 시나리오 테스트")
    class Extract_Keyword_Scenario {

        @Test
        @DisplayName("특수문자가 포함된 정보가 주어지면 순수 단어만 정갈하게 추출하여 반환한다")
        void givenSpecialCharacters_whenExtract_thenReturnsCleanedKeywords() {
            // given
            String brand = "[나이키]";
            String title = "(특전) 에어포스 1-중판";

            // when
            List<String> result = couponKeywordManager.extract(brand, title);

            // then
            assertThat(result).containsExactlyInAnyOrder("나이키", "특전", "에어포스", "중판");
            assertThat(result).doesNotContain("[나이키]", "(특전)", "1-중판");
        }

        @Test
        @DisplayName("중복된 키워드가 포함된 정보가 주어지면 중복을 제거하고 하나의 키워드만 반환한다")
        void givenDuplicateKeywords_whenExtract_thenRemovesDuplicates() {
            // given
            String text1 = "나이키 신발";
            String text2 = "신발 세일";

            // when
            List<String> result = couponKeywordManager.extract(text1, text2);

            // then
            assertThat(result).hasSize(3)
                    .containsExactlyInAnyOrder("나이키", "신발", "세일");
        }

        @Test
        @DisplayName("2글자 미만의 짧은 키워드가 주어지면 인덱싱에서 제외하고 반환한다")
        void givenShortWords_whenExtract_thenFiltersShortWords() {
            // given
            String text = "앱 전용 덤 템";

            // when
            List<String> result = couponKeywordManager.extract(text);

            // then
            assertThat(result).containsExactly("전용");
            assertThat(result).doesNotContain("앱", "덤", "템");
        }

        @Test
        @DisplayName("불규칙한 공백이 포함된 정보가 주어지면 정확하게 토큰을 분리하여 반환한다")
        void givenIrregularSpaces_whenExtract_thenHandlesIrregularSpaces() {
            // given
            String text = "  나이키    에어포스  ";

            // when
            List<String> result = couponKeywordManager.extract(text);

            // then
            assertThat(result).containsExactlyInAnyOrder("나이키", "에어포스");
        }
    }

}
