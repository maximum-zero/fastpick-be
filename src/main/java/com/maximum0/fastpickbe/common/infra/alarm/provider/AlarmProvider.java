package com.maximum0.fastpickbe.common.infra.alarm.provider;

import com.maximum0.fastpickbe.common.exception.ErrorCode;

public interface AlarmProvider {
    void sendErrorAlert(Exception e, String traceId, String method, String uri, ErrorCode errorCode);
}
