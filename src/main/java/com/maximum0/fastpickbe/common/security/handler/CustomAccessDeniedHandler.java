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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 권한이 없는 사용자의 접근 시도를 처리하는 핸들러.
 * HTTP 403 Forbidden 응답을 {@link ErrorResponse} 규격으로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        log.warn("⚠️ [AccessDeniedException] 접근 거부 - {} {} | Params: {}",
                request.getMethod(), request.getRequestURI(), RequestUtils.getRequestParams(request));

        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.FORBIDDEN);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(ErrorCode.FORBIDDEN.getStatus());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}
