package com.maximum0.fastpickbe.common.exception;

import com.maximum0.fastpickbe.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리를 담당하는 어드바이스 클래스.
 * 예외 발생 시 {@link ErrorResponse} 규격으로 응답을 통일한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 유효성 검증 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("⚠️ [MethodArgumentNotValidException]: {} {} | Params: {}",
                request.getMethod(), request.getRequestURI(), getRequestParams(request));

        final ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, e.getBindingResult()));
    }

    /**
     * 주로 요청 파라미터의 타입이 일치하지 않을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        log.warn("⚠️ [MethodArgumentTypeMismatchException]: {} {} - {} | Params: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), getRequestParams(request));

        final ErrorCode errorCode = ErrorCode.INVALID_TYPE_VALUE;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 비즈니스 로직 수행 중 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(
            final BusinessException e,
            HttpServletRequest request
    ) {
        log.warn("⚠️ [BusinessException]: {} {} - {} | Params: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), getRequestParams(request));

        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 처리되지 않은 모든 예외를 최종적으로 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("⛔️ [Exception]: {} {} - {} | Params: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), getRequestParams(request), e);

        final ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * HttpServletRequest의 파라미터 맵을 로깅을 위한 문자열로 변환합니다.
     * @param request 현재 HTTP 요청
     * @return 로깅용으로 포맷팅된 파라미터 문자열
     */
    private String getRequestParams(HttpServletRequest request) {
        var parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) return "[]";
        return "[" + parameterMap.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + String.join(",", entry.getValue()))
                .collect(java.util.stream.Collectors.joining(", ")) + "]";
    }

}
