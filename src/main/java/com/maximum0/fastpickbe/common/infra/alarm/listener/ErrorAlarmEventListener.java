package com.maximum0.fastpickbe.common.infra.alarm.listener;

import com.maximum0.fastpickbe.common.event.ErrorOccurredEvent;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.exception.ErrorPolicy;
import com.maximum0.fastpickbe.common.infra.alarm.provider.AlarmProvider;
import com.maximum0.fastpickbe.common.logging.MdcKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorAlarmEventListener {

    private final AlarmProvider alarmProvider;

    /**
     * ErrorOccurredEvent를 수신하여 알람 전송 여부를 판단하고,
     * 정책에 따라 외부 알람 채널로 에러 정보를 전달한다.
     */
    @EventListener
    public void handle(ErrorOccurredEvent event) {
        try {
            ErrorCode errorCode = event.errorCode();
            ErrorPolicy policy = ErrorPolicy.from(errorCode.getLevel());
            if (!policy.alertable()) {
                return;
            }

            alarmProvider.sendErrorAlert(
                    event.exception(),
                    MDC.get(MdcKeys.TRACE_ID),
                    event.request().getMethod(),
                    event.request().getRequestURI(),
                    errorCode
            );
        } catch (Exception e) {
            log.error("⛔️ [ErrorAlarmEventListener] 알람 이벤트 처리 중 예외 발생", e);
        }
    }

}
