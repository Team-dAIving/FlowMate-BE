package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.entity.GithubWebhookDelivery;
import kr.co.daeun.flowmatebe.domain.github.repository.GithubWebhookDeliveryRepository;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubWebhookService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final GithubWebhookDeliveryRepository deliveryRepository;
    private final GithubAppProperties githubAppProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public void receive(
            String deliveryId,
            String eventName,
            String suppliedSignature,
            byte[] requestBody
    ) {
        validateHeaders(deliveryId, eventName);
        verifySignature(suppliedSignature, requestBody);

        if (deliveryRepository.existsById(deliveryId)) {
            return;
        }

        Map<String, Object> payload = readPayload(requestBody);
        GithubWebhookDelivery delivery = new GithubWebhookDelivery();
        delivery.setDeliveryId(deliveryId);
        delivery.setEventName(eventName);
        delivery.setAction(stringValue(payload.get("action")));
        delivery.setGithubRepositoryId(repositoryId(payload));
        delivery.setPayload(payload);
        delivery.setProcessingStatus("RECEIVED");
        deliveryRepository.save(delivery);
    }

    private void validateHeaders(String deliveryId, String eventName) {
        if (!StringUtils.hasText(deliveryId) || !StringUtils.hasText(eventName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GitHub webhook 헤더가 올바르지 않습니다."
            );
        }
    }

    private void verifySignature(String suppliedSignature, byte[] requestBody) {
        String webhookSecret = githubAppProperties.webhookSecret();
        if (!StringUtils.hasText(webhookSecret)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub webhook secret이 설정되지 않았습니다."
            );
        }
        if (!StringUtils.hasText(suppliedSignature)) {
            throw invalidSignature();
        }

        String expectedSignature = SIGNATURE_PREFIX + HexFormat.of().formatHex(
                sign(webhookSecret, requestBody)
        );
        boolean matches = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                suppliedSignature.getBytes(StandardCharsets.US_ASCII)
        );
        if (!matches) {
            throw invalidSignature();
        }
    }

    private byte[] sign(String secret, byte[] requestBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return mac.doFinal(requestBody);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("GitHub webhook 서명을 검증할 수 없습니다.", exception);
        }
    }

    private Map<String, Object> readPayload(byte[] requestBody) {
        try {
            return objectMapper.readValue(requestBody, PAYLOAD_TYPE);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GitHub webhook payload를 읽을 수 없습니다."
            );
        }
    }

    private Long repositoryId(Map<String, Object> payload) {
        Object repository = payload.get("repository");
        if (!(repository instanceof Map<?, ?> repositoryMap)) {
            return null;
        }
        Object id = repositoryMap.get("id");
        if (id instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String stringValue(Object value) {
        return value instanceof String string ? string : null;
    }

    private ResponseStatusException invalidSignature() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "GitHub webhook 서명이 올바르지 않습니다."
        );
    }
}
