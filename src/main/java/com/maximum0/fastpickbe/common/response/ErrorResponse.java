package com.maximum0.fastpickbe.common.response;

import com.maximum0.fastpickbe.common.exception.ErrorCode;
import java.util.List;
import org.springframework.validation.BindingResult;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> errors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), FieldError.from(bindingResult));
    }

    public record FieldError(String field, String value, String reason) {
        public static List<FieldError> from(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(error -> {
                        error.getRejectedValue();
                        return new FieldError(
                                error.getField(),
                                error.getRejectedValue().toString(),
                                error.getDefaultMessage());
                    })
                    .toList();
        }
    }
}
