package com.maximum0.fastpickbe.coupon.application.admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KeywordExtractor {
    /**
     * 브랜드명과 타이틀에서 검색용 키워드를 추출합니다.
     * @param texts 추출 대상 문자열 (brand, title 등)
     * @return 중복이 제거된 키워드 리스트
     */
    public List<String> extract(String... texts) {
        return Arrays.stream(texts)
                .filter(StringUtils::hasText)
                .map(this::cleanse)
                .flatMap(text -> Arrays.stream(text.split("\\s+")))
                .map(String::trim)
                .filter(word -> word.length() >= 2)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 한글, 영문, 숫자 외의 특수문자를 공백으로 치환합니다.
     * "[특가]나이키" -> " 특가 나이키" -> split에 의해 정갈하게 분리됨
     */
    private String cleanse(String text) {
        if (!StringUtils.hasText(text)) return "";
        return text.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]", " ");
    }
}
