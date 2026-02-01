package com.maximum0.fastpickbe.common.exception;

import com.maximum0.fastpickbe.common.infra.alarm.AlarmSeverity;

public record ErrorPolicy(
        boolean alertable,
        AlarmSeverity severity,
        int color
) {

    public static ErrorPolicy from(ErrorLevel level) {
        return switch (level) {
            case INFO  -> info();
            case WARN  -> warn();
            case ERROR -> critical();
        };
    }

    /**
     * [완전히 무시되는 정책]
     * - 알람 발송 ❌
     * - 로그/메트릭 외에는 어떤 대응도 하지 않음
     */
    public static ErrorPolicy silent() {
        return new ErrorPolicy(false, AlarmSeverity.INFO, 0xAAAAAA);
    }

    /**
     * [정보성 정책]
     * - 정상적인 비즈니스 흐름에서 발생 가능한 실패
     * - 사용자 행동, 정책 위반, 단순 오류 등
     */
    public static ErrorPolicy info() {
        return new ErrorPolicy(false, AlarmSeverity.INFO, 0xAAAAAA);
    }

    /**
     * [경고 정책]
     * - 시스템은 정상이나 비정상적인 패턴이 감지됨
     * - 지속 발생 시 장애로 이어질 가능성 있음
     */
    public static ErrorPolicy warn() {
        return new ErrorPolicy(true, AlarmSeverity.WARN, 0xFFA500);
    }

    /**
     * [오류 정책]
     * - 즉각적인 확인이 필요한 시스템 오류
     * - 일부 기능 장애 가능성 존재
     */
    public static ErrorPolicy error() {
        return new ErrorPolicy(true, AlarmSeverity.ERROR, 0xFF0000);
    }

    /**
     * [치명적 오류 정책]
     * - 시스템 신뢰성에 직접적인 영향을 미침
     * - 즉각 조치 및 담당자 호출 대상
     */
    public static ErrorPolicy critical() {
        return new ErrorPolicy(true, AlarmSeverity.CRITICAL, 0x000000);
    }

}