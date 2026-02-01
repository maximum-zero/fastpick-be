package com.maximum0.fastpickbe.common.utils;

import jakarta.servlet.http.HttpServletRequest;

public class RequestUtils {

    /**
     * HttpServletRequest의 파라미터 맵을 문자열로 변환한다.
     *
     * @param request 현재 HTTP 요청 객체
     * @return String 로깅용으로 포맷팅된 파라미터 문자열
     */
    public static String getRequestParams(HttpServletRequest request) {
        var parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) return "[]";

        return "[" + parameterMap.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + String.join(",", entry.getValue()))
                .collect(java.util.stream.Collectors.joining(", ")) + "]";
    }

}
