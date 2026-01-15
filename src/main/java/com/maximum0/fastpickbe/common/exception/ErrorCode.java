package com.maximum0.fastpickbe.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT_VALUE(400, "C001", "올바르지 않은 입력값입니다."),
    INVALID_TYPE_VALUE(400, "C002", "입력 타입이 일치하지 않습니다."),
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류가 발생했습니다."),

    // 유저/인증 (Auth)
    USER_NOT_FOUND(404, "U001", "존재하지 않는 사용자입니다."),
    INVALID_TOKEN(401, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "A002", "만료된 토큰입니다."),

    // 쿠폰 (Coupon)
    COUPON_NOT_FOUND(404, "CP01", "존재하지 않는 쿠폰입니다."),
    COUPON_DISABLED(400, "CP02", "사용 중지된 쿠폰입니다."),
    COUPON_NOT_AVAILABLE_PERIOD(400, "CP03", "쿠폰 발급 기간이 아닙니다."),
    COUPON_EXHAUSTED(400, "CP04", "쿠폰 수량이 모두 소진되었습니다."),
    ALREADY_ISSUED_COUPON(400, "CP05", "이미 발급받은 쿠폰입니다.");

    private final int status;
    private final String code;
    private final String message;
}