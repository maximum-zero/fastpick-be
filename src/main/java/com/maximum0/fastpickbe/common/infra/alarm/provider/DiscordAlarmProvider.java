package com.maximum0.fastpickbe.common.infra.alarm.provider;

import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.exception.ErrorPolicy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Profile("prod")
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordAlarmProvider implements AlarmProvider {

    @Value("${discord.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 에러 정보를 Discord 웹훅으로 전송한다.
     *
     * @param e 발생한 예외 객체
     * @param traceId 요청 추적을 위한 ID
     * @param method HTTP 요청 메서드
     * @param uri 요청된 리소스의 URI
     * @param errorCode 발생한 에러의 종류를 나타내는 코드
     */
    @Override
    public void sendErrorAlert(Exception e, String traceId, String method, String uri, ErrorCode errorCode) {
        try {
            ErrorPolicy policy = errorCode.getPolicy();

            String title = String.format("[%s] %s", policy.severity(), errorCode.getCode());
            String description = String.format(
                    "**🚨 에러 요약**\n" +
                    "- **코드**: %s\n" +
                    "- **의미**: %s\n\n" +

                    "**📍 요청 정보**\n" +
                    "- **API**: %s %s\n\n" +

                    "**🧩 예외 정보**\n" +
                    "- **Exception**: %s\n" +
                    "- **Message**:\n" +
                    "```\n%s\n```\n\n" +

                    "**🧵 추적 정보**\n" +
                    "- **Trace ID**:\n" +
                    "```\n%s\n```",
                    errorCode.getCode(),
                    errorCode.getMessage(),
                    method,
                    uri,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    traceId
            );

            var payload = new DiscordPayload(null, List.of(new DiscordEmbed(title, description, policy.color())));

            restTemplate.postForEntity(webhookUrl, payload, String.class);
        } catch (Exception ex) {
            log.error("⛔️ [DiscordAlarmProvider] [{}] {} {} - 알람 발송 실패 | Message: {}",
                    traceId, method, uri, ex.getMessage());
        }
    }

    private record DiscordPayload(String content, List<DiscordEmbed> embeds) {}
    private record DiscordEmbed(String title, String description, int color) {}

}
