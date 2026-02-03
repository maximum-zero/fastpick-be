package com.maximum0.fastpickbe.common.exception;

import com.maximum0.fastpickbe.common.event.ErrorOccurredEvent;
import com.maximum0.fastpickbe.common.response.ErrorResponse;
import com.maximum0.fastpickbe.common.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리 Entry Point.
 *
 * - 모든 예외는 ErrorCode 기반으로 표준화된 {@link ErrorResponse} 형태로 변환한다.
 * - {@link ErrorLevel}을 기준으로 로깅 및 알람 정책이 결정된다.
 * - 실제 알람 전송은 이벤트 발행을 통해 infra 영역으로 위임한다.
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;


    /**
     * 클라이언트의 요청 데이터 유효성 검증 실패 시 발생하는 예외를 처리한다.
     * - MethodArgumentNotValidException: @RequestBody 유효성 실패
     * - ConstraintViolationException: 쿼리 파라미터, 경로 변수 유효성 실패
     * - HttpMessageNotReadableException: JSON 파싱 실패 또는 바디 누락
     * - MethodArgumentTypeMismatchException: 파라미터 타입 불일치
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    protected ResponseEntity<ErrorResponse> handleValidationExceptions(Exception e, HttpServletRequest request) {
        if (e instanceof MethodArgumentTypeMismatchException ex) {
            String detail = String.format("필드 '%s'의 값 '%s'이(가) 유효하지 않습니다.", ex.getName(), ex.getValue());
            return handleExceptionInternal(e, ErrorCode.INVALID_TYPE_VALUE, request, null, detail);
        }

        if (e instanceof HttpMessageNotReadableException) {
            return handleExceptionInternal(e, ErrorCode.INVALID_INPUT_VALUE, request, null, null);
        }

        BindingResult bindingResult = (e instanceof MethodArgumentNotValidException ex) ? ex.getBindingResult() : null;
        return handleExceptionInternal(e, ErrorCode.INVALID_INPUT_VALUE, request, bindingResult, null);
    }

    /**
     * 서비스 계층의 비즈니스 규칙 위반 시 발생하는 BusinessException을 처리한다.
     * 정의된 ErrorCode에 따라 상태 코드와 메시지를 결정한다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        return handleExceptionInternal(e, e.getErrorCode(), request, null, null);
    }

    /**
     * 시스템 내부에서 정의되지 않은 모든 예외를 처리한다.
     * 예상치 못한 런타임 예외 발생 시 500 Internal Server Error를 반환한다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        return handleExceptionInternal(e, ErrorCode.INTERNAL_SERVER_ERROR, request, null, null);
    }

    /**
     * 예외의 성격(ErrorLevel)에 따라 로깅을 수행하고 최종 응답 객체를 생성한다.
     */
    private ResponseEntity<ErrorResponse> handleExceptionInternal(
            Exception e,
            ErrorCode errorCode,
            HttpServletRequest request,
            BindingResult bindingResult,
            String detail
    ) {
        ErrorResponse response = (bindingResult != null)
                ? ErrorResponse.of(errorCode, bindingResult)
                : (detail != null) ? ErrorResponse.of(errorCode, detail) : ErrorResponse.of(errorCode);

        String errorContext = (bindingResult != null && !bindingResult.getFieldErrors().isEmpty())
                ? String.format("[%s: %s]", bindingResult.getFieldErrors().get(0).getField(), bindingResult.getFieldErrors().get(0).getDefaultMessage())
                : response.message();

        String logMessage = String.format("[%s] %s %s - %s | Params: %s",
                e.getClass().getSimpleName(), request.getMethod(), request.getRequestURI(), errorContext, RequestUtils.getRequestParams(request));

        switch (errorCode.getLevel()) {
            case ERROR -> log.error("⛔️ " + logMessage, e);
            case WARN  -> log.warn("⚠️ " + logMessage);
            default    -> log.info("ℹ️ " + logMessage);
        }

        ErrorPolicy policy = ErrorPolicy.from(errorCode.getLevel());
        if (policy.alertable()) {
            eventPublisher.publishEvent(new ErrorOccurredEvent(e, errorCode, request));
        }

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

}
