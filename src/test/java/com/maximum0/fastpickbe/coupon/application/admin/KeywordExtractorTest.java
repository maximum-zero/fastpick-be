package com.maximum0.fastpickbe.coupon.application.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KeywordExtractor 단위 테스트")
class KeywordExtractorTest {
    private final KeywordExtractor keywordExtractor = new KeywordExtractor();

    @Nested
    @DisplayName("키워드 추출 테스트")
    class ExtractBehaviorTest {

        @Test
        @DisplayName("특수문자가 포함된 경우 순수 단어만 정갈하게 추출한다")
        void KeywordExtractor_Extract_ReturnsCleanedKeywords() {
            // given
            String brand = "[나이키]";
            String title = "(특전) 에어포스 1-중판";

            // when
            List<String> result = keywordExtractor.extract(brand, title);

            // then
            assertThat(result).containsExactlyInAnyOrder("나이키", "특전", "에어포스", "중판");
            assertThat(result).doesNotContain("[나이키]", "(특전)", "1-중판");
        }

        @Test
        @DisplayName("여러 텍스트에 중복된 키워드가 있어도 하나만 남긴다")
        void KeywordExtractor_Extract_RemovesDuplicates() {
            // given
            String text1 = "나이키 신발";
            String text2 = "신발 세일";

            // when
            List<String> result = keywordExtractor.extract(text1, text2);

            // then
            assertThat(result).hasSize(3)
                    .containsExactlyInAnyOrder("나이키", "신발", "세일");
        }

        @Test
        @DisplayName("2글자 미만의 짧은 키워드는 인덱싱에서 제외한다")
        void KeywordExtractor_Extract_FiltersShortWords() {
            // given
            String text = "앱 전용 덤 템";

            // when
            List<String> result = keywordExtractor.extract(text);

            // then
            assertThat(result).containsExactly("전용");
            assertThat(result).doesNotContain("앱", "덤", "템");
        }

        @Test
        @DisplayName("불규칙한 공백이 포함되어도 정확하게 토큰을 분리한다")
        void KeywordExtractor_Extract_HandlesIrregularSpaces() {
            // given
            String text = "  나이키    에어포스  ";

            // when
            List<String> result = keywordExtractor.extract(text);

            // then
            assertThat(result).containsExactlyInAnyOrder("나이키", "에어포스");
        }
    }
}