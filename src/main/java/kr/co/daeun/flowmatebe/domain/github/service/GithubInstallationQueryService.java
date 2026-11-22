package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.dto.response.GithubInstallationListResponse;
import kr.co.daeun.flowmatebe.domain.github.entity.GithubWebhookDelivery;
import kr.co.daeun.flowmatebe.domain.github.repository.GithubWebhookDeliveryRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import kr.co.daeun.flowmatebe.global.github.dto.GithubInstallationRepositories;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubInstallationQueryService {

    private final UserRepository userRepository;
    private final GithubWebhookDeliveryRepository deliveryRepository;
    private final GithubInstallationClient installationClient;

    public GithubInstallationListResponse getMyInstallations(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "인증된 사용자를 찾을 수 없습니다."
                ));
        if (user.getGithubUserId() == null) {
            return new GithubInstallationListResponse(List.of());
        }

        Map<Long, InstallationEvent> installations = new LinkedHashMap<>();
        for (GithubWebhookDelivery delivery : deliveryRepository
                .findAllByEventNameAndActionOrderByReceivedAtDesc("installation", "created")) {
            InstallationEvent event = installationEvent(delivery.getPayload());
            if (event != null
                    && user.getGithubUserId().equals(event.senderGithubUserId())) {
                installations.putIfAbsent(event.installationId(), event);
            }
        }

        List<GithubInstallationListResponse.Installation> responses = new ArrayList<>();
        for (InstallationEvent installation : installations.values()) {
            GithubInstallationRepositories repositories =
                    installationClient.listRepositories(installation.installationId());
            responses.add(new GithubInstallationListResponse.Installation(
                    installation.installationId(),
                    installation.accountLogin(),
                    installation.accountType(),
                    repositories.repositories()
            ));
        }
        return new GithubInstallationListResponse(List.copyOf(responses));
    }

    private InstallationEvent installationEvent(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object installationValue = payload.get("installation");
        Object senderValue = payload.get("sender");
        if (!(installationValue instanceof Map<?, ?> installation)
                || !(senderValue instanceof Map<?, ?> sender)) {
            return null;
        }
        Long installationId = longValue(installation.get("id"));
        Long senderId = longValue(sender.get("id"));
        Object accountValue = installation.get("account");
        if (installationId == null || senderId == null
                || !(accountValue instanceof Map<?, ?> account)) {
            return null;
        }
        return new InstallationEvent(
                installationId,
                senderId,
                stringValue(account.get("login")),
                stringValue(account.get("type"))
        );
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String stringValue(Object value) {
        return value instanceof String string ? string : null;
    }

    private record InstallationEvent(
            Long installationId,
            Long senderGithubUserId,
            String accountLogin,
            String accountType
    ) {
    }
}
