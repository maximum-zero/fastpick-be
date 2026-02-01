package com.maximum0.fastpickbe.common.infra.alarm;

/**
 * 알람의 심각도를 정의하는 열거형.
 * 이 값은 에러 정책(ErrorPolicy)과 연계되어 알람 채널(e.g., Discord)로 전달될 때
 * 메시지의 시각적 표현(색상, 제목)을 결정하는 데 사용된다.
 */
public enum AlarmSeverity {

    INFO,
    WARN,
    ERROR,
    CRITICAL

}
