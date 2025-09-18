package org.solace.scholar_ai.project_service.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserNotificationClient {

    private final RestTemplate restTemplate;

    @Value("${scholarai.spring.user-service-url}")
    private String userServiceBaseUrl;

    public void send(UUID userId, String notificationType, Map<String, Object> templateData) {
        try {
            String url = userServiceBaseUrl + "/api/v1/notifications/send";
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("notificationType", notificationType);
            payload.put("templateData", templateData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Notification {} sent to user {} via user-service", notificationType, userId);
        } catch (Exception e) {
            log.warn("Failed to send notification {} to user {}: {}", notificationType, userId, e.getMessage());
        }
    }
}
