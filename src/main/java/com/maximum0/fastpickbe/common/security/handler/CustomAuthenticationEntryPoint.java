package com.maximum0.fastpickbe.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.response.ErrorResponse;
import com.maximum0.fastpickbe.common.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 사용자의 접근 시도를 처리하는 엔트리 포인트.
 * HTTP 401 Unauthorized 응답을 {@link ErrorResponse} 규격으로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        log.warn("⚠️ [AuthenticationEntryPoint] 인증 실패 - {} {} | reason={} | params={}",
                request.getMethod(), request.getRequestURI(), authException.getMessage(), RequestUtils.getRequestParams(request));

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}
