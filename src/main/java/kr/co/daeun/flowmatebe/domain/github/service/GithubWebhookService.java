package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.entity.GithubWebhookDelivery;
import kr.co.daeun.flowmatebe.domain.github.repository.GithubWebhookDeliveryRepository;
<<<<<<< Updated upstream
import kr.co.daeun.flowmatebe.domain.github.repository.TeamGithubInstallationRepository;
=======
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
>>>>>>> Stashed changes
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
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubWebhookService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String ISSUES_EVENT = "issues";
    private static final String CLOSED_ACTION = "closed";
    private static final String REOPENED_ACTION = "reopened";
    private static final String PENDING = "PENDING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";
    private static final String IGNORED = "IGNORED";
    private static final Duration UNMATCHED_RETRY_WINDOW = Duration.ofMinutes(5);
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final GithubWebhookDeliveryRepository deliveryRepository;
<<<<<<< Updated upstream
    private final TeamGithubInstallationRepository teamInstallationRepository;
=======
    private final TaskRepository taskRepository;
>>>>>>> Stashed changes
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
        delivery.setProcessingStatus(PENDING);
        deliveryRepository.save(delivery);

<<<<<<< Updated upstream
        if ("installation".equals(eventName) && "deleted".equals(delivery.getAction())) {
            Long installationId = installationId(payload);
            if (installationId != null) {
                teamInstallationRepository.deleteAllByInstallationId(installationId);
            }
        }
=======
        if (isIssueStateChange(delivery)) {
            processIssueStateChange(delivery);
        }
    }

    @Transactional
    public void processPendingIssueDelivery(String deliveryId) {
        deliveryRepository.findById(deliveryId)
                .filter(delivery -> PENDING.equals(delivery.getProcessingStatus()))
                .filter(this::isIssueStateChange)
                .ifPresent(this::processIssueStateChange);
    }

    private void processIssueStateChange(GithubWebhookDelivery delivery) {
        Integer issueNumber = issueNumber(delivery.getPayload());
        if (delivery.getGithubRepositoryId() == null || issueNumber == null) {
            finish(delivery, FAILED, "GitHub 저장소 또는 이슈 번호가 없습니다.");
            return;
        }

        List<Task> tasks = taskRepository
                .findAllByGithubRepository_GithubRepositoryIdAndGithubIssueNumber(
                        delivery.getGithubRepositoryId(),
                        issueNumber
                );
        if (tasks.isEmpty()) {
            if (retryWindowExpired(delivery)) {
                finish(delivery, IGNORED, "연결된 업무를 찾을 수 없습니다.");
            }
            return;
        }

        if (CLOSED_ACTION.equals(delivery.getAction())) {
            Instant completedAt = issueClosedAt(delivery.getPayload());
            tasks.forEach(task -> {
                task.setStatus("DONE");
                task.setCompletedAt(completedAt);
            });
        } else {
            tasks.forEach(task -> {
                task.setStatus("TODO");
                task.setCompletedAt(null);
            });
        }
        finish(delivery, COMPLETED, null);
    }

    private boolean isIssueStateChange(GithubWebhookDelivery delivery) {
        return ISSUES_EVENT.equals(delivery.getEventName())
                && (CLOSED_ACTION.equals(delivery.getAction())
                || REOPENED_ACTION.equals(delivery.getAction()));
    }

    private Integer issueNumber(Map<String, Object> payload) {
        Object issue = payload.get("issue");
        if (!(issue instanceof Map<?, ?> issueMap)) {
            return null;
        }
        Object number = issueMap.get("number");
        return number instanceof Number value ? value.intValue() : null;
    }

    private Instant issueClosedAt(Map<String, Object> payload) {
        Object issue = payload.get("issue");
        if (issue instanceof Map<?, ?> issueMap
                && issueMap.get("closed_at") instanceof String closedAt) {
            try {
                return Instant.parse(closedAt);
            } catch (RuntimeException ignored) {
                // GitHub normally supplies an ISO-8601 instant. Use receipt time if it does not.
            }
        }
        return Instant.now();
    }

    private boolean retryWindowExpired(GithubWebhookDelivery delivery) {
        return delivery.getReceivedAt() != null
                && delivery.getReceivedAt().isBefore(
                Instant.now().minus(UNMATCHED_RETRY_WINDOW)
        );
    }

    private void finish(GithubWebhookDelivery delivery, String status, String errorMessage) {
        delivery.setProcessingStatus(status);
        delivery.setErrorMessage(errorMessage);
        delivery.setProcessedAt(Instant.now());
>>>>>>> Stashed changes
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

    private Long installationId(Map<String, Object> payload) {
        Object installation = payload.get("installation");
        if (!(installation instanceof Map<?, ?> installationMap)) {
            return null;
        }
        Object id = installationMap.get("id");
        return id instanceof Number number ? number.longValue() : null;
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
