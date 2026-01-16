package com.maximum0.fastpickbe.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    /**
     * 시스템 전반에서 사용할 시간 공급자(Clock)를 빈으로 등록합니다.
     * new Date() 또는 LocalDateTime.now()를 직접 사용하는 대신 Clock을 주입받아 사용하면,
     * 테스트 코드에서 시간을 고정하거나 조작(Mocking)할 수 있어 비즈니스 로직 검증이 용이해집니다.
     * * @return 시스템 기본 시간대를 사용하는 Clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
