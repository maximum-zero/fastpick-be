package com.maximum0.fastpickbe.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(400, "C001", "올바르지 않은 입력값입니다.", ErrorLevel.INFO),
    INVALID_TYPE_VALUE(400, "C002", "입력 타입이 일치하지 않습니다.", ErrorLevel.INFO),
    UNAUTHORIZED(401, "C003", "인증이 필요한 서비스입니다.", ErrorLevel.INFO),
    FORBIDDEN(403, "C004", "권한이 없는 사용자입니다.", ErrorLevel.INFO),
    INTERNAL_SERVER_ERROR(500, "C005", "서버 내부 오류가 발생했습니다.", ErrorLevel.ERROR),

    // 유저/인증
    USER_NOT_FOUND(404, "U001", "존재하지 않는 사용자입니다.", ErrorLevel.INFO),
    DUPLICATE_EMAIL(409, "U002", "이미 존재하는 이메일입니다.", ErrorLevel.INFO),
    INVALID_TOKEN(401, "A001", "유효하지 않은 토큰입니다.", ErrorLevel.INFO),
    EXPIRED_TOKEN(401, "A002", "만료된 토큰입니다.", ErrorLevel.INFO),
    LOGIN_FAILED(401, "A003", "아이디 또는 비밀번호가 일치하지 않습니다.", ErrorLevel.INFO),

    // 쿠폰
    COUPON_NOT_FOUND(404, "CP01", "존재하지 않는 쿠폰입니다.", ErrorLevel.WARN),
    COUPON_DISABLED(400, "CP02", "사용 중지된 쿠폰입니다.", ErrorLevel.WARN),
    COUPON_NOT_AVAILABLE_PERIOD(400, "CP03", "쿠폰 발급 기간이 아닙니다.", ErrorLevel.INFO),
    COUPON_EXHAUSTED(400, "CP04", "쿠폰 수량이 모두 소진되었습니다.", ErrorLevel.INFO),
    ALREADY_ISSUED_COUPON(400, "CP05", "이미 발급받은 쿠폰입니다.", ErrorLevel.INFO),

    // 시스템/동시성 제어
    CONCURRENCY_BUSY(429, "S001", "현재 접속자가 많아 요청을 처리할 수 없습니다.", ErrorLevel.WARN),
    SYSTEM_LOCKING_ERROR(500, "S002", "요청 처리 중 시스템 오류가 발생했습니다.", ErrorLevel.ERROR)
    ;

    private final int status;
    private final String code;
    private final String message;
    private final ErrorLevel level;

}
