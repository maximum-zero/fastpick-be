package com.maximum0.fastpickbe.common.logging.filter;

import com.maximum0.fastpickbe.common.logging.MdcKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 모든 HTTP 요청에 대해 고유한 트랜잭션 추적 ID(Trace ID)를 부여하는 서블릿 필터.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 요청이 들어올 때마다 MDC에 고유한 `traceId`를 설정하고, 요청 처리가 끝나면 제거한다.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MdcKeys.TRACE_ID, traceId);

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.TRACE_ID);
        }
    }

}
