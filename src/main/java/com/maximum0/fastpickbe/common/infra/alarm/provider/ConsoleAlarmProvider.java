package com.maximum0.fastpickbe.common.infra.alarm.provider;

import com.maximum0.fastpickbe.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"})
public class ConsoleAlarmProvider implements AlarmProvider {

    /**
     * 에러 정보를 콘솔에 INFO 레벨 로그로 출력한다.
     * 실제 알람은 발송하지 않고, 시뮬레이션 역할만 수행한다.
     *
     * @param e 발생한 예외 객체
     * @param traceId 요청 추적을 위한 ID
     * @param method HTTP 요청 메서드
     * @param uri 요청된 리소스의 URI
     * @param errorCode 발생한 에러의 종류를 나타내는 코드
     */
    @Override
    public void sendErrorAlert(Exception e, String traceId, String method, String uri, ErrorCode errorCode) {
        log.info("✅ [ConsoleAlarmProvider] 알람 발송 생략 (로컬 모드) - {} {} | Message: {}",
                method, uri, e.getMessage());
    }
}