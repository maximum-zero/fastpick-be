package com.maximum0.fastpickbe.common.response;

public record ApiResponse<T>(
        String code,
        String message,
        T data
) {
    public static final String SUCCESS_CODE = "S000";
    public static final String SUCCESS_MESSAGE = "Success";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

}
