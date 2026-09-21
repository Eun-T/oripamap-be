package org.scoula.emailverification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scoula.emailverification.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ResendEmailClient {
    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";
    private static final String SUBJECT = "[오맵] 이메일 인증번호입니다";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:}")
    private String fromEmail;

    public void sendVerificationCode(String recipient, String code) {
        if (apiKey == null || apiKey.isBlank() || fromEmail == null || fromEmail.isBlank()) {
            throw new EmailDeliveryException("이메일 발송 설정이 완료되지 않았습니다.");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("from", formatFromAddress(fromEmail.trim()));
        ArrayNode recipients = body.putArray("to");
        recipients.add(recipient);
        body.put("subject", SUBJECT);
        body.put("text", "오맵 회원가입을 위한 인증번호입니다.\n\n"
                + code + "\n\n인증번호는 5분 동안 유효합니다.");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(
                    RESEND_EMAILS_URL,
                    new HttpEntity<>(body, headers),
                    String.class);
        } catch (RestClientException e) {
            throw new EmailDeliveryException("인증메일 발송에 실패했습니다.", e);
        }
    }

    private String formatFromAddress(String configuredAddress) {
        return configuredAddress.contains("<")
                ? configuredAddress
                : "오맵 <" + configuredAddress + ">";
    }
}
