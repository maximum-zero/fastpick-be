package com.maximum0.fastpickbe.coupon.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CouponKeywordManager {

    /**
     * 입력된 여러 텍스트(브랜드명, 제목 등)로부터 검색용 키워드를 추출한다.
     * 특수문자 제거, 공백 분리, 최소 길이 필터링 및 중복 제거 과정을 거친다.
     *
     * @param texts 추출 대상 문자열 가변 인자 (brand, title 등)
     * @return 정제된 키워드 리스트 (중복 제거 및 2글자 이상)
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
     * 한글, 영문, 숫자 외의 특수문자를 공백으로 치환하여 단어 경계를 명확히 한다.
     * 예: "[특가]나이키" -> " 특가 나이키"
     *
     * @param text 정제할 원본 문자열
     * @return 특수문자가 공백으로 치환된 문자열
     */
    private String cleanse(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]", " ");
    }

}
