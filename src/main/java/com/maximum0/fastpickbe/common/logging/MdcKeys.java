package com.maximum0.fastpickbe.common.logging;

/**
 * MDC(Mapped Diagnostic Context)에 사용되는 공통 키 상수 정의.
 *
 * - 로그, 필터, 인터셉터, 비동기 처리 등에서 공통으로 사용된다.
 */
public final class MdcKeys {

    private MdcKeys() {}

    public static final String TRACE_ID = "traceId";

}
