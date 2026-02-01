package com.maximum0.fastpickbe.common.event;

import com.maximum0.fastpickbe.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 시스템 내에서 에러가 발생했음을 알리기 위한 도메인 이벤트.
 */
public record ErrorOccurredEvent(
        Exception exception,
        ErrorCode errorCode,
        HttpServletRequest request
) {
}