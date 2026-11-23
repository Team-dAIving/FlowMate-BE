package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.entity.GithubWebhookDelivery;
import kr.co.daeun.flowmatebe.domain.github.repository.GithubWebhookDeliveryRepository;
import kr.co.daeun.flowmatebe.domain.github.repository.TeamGithubInstallationRepository;
import kr.co.daeun.flowmatebe.global.github.config.GithubAppProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubWebhookServiceTest {

    private static final String SECRET = "test-webhook-secret";

    private final GithubWebhookDeliveryRepository repository =
            mock(GithubWebhookDeliveryRepository.class);
    private final TeamGithubInstallationRepository teamInstallationRepository =
            mock(TeamGithubInstallationRepository.class);
    private final GithubWebhookService service = new GithubWebhookService(
            repository,
            teamInstallationRepository,
            new GithubAppProperties(
                    "flowmate-integration",
                    5_876_834L,
                    "private-key",
                    SECRET,
                    Duration.ofMinutes(10)
            ),
            new ObjectMapper()
    );

    @Test
    void 유효한_GitHub_webhook을_저장한다() throws Exception {
        byte[] payload = """
                {
                  "action": "review_requested",
                  "repository": {"id": 12345},
                  "pull_request": {"number": 17}
                }
                """.getBytes(StandardCharsets.UTF_8);
        when(repository.existsById("delivery-1")).thenReturn(false);

        service.receive("delivery-1", "pull_request", signature(payload), payload);

        ArgumentCaptor<GithubWebhookDelivery> captor =
                ArgumentCaptor.forClass(GithubWebhookDelivery.class);
        verify(repository).save(captor.capture());
        GithubWebhookDelivery saved = captor.getValue();
        assertThat(saved.getDeliveryId()).isEqualTo("delivery-1");
        assertThat(saved.getEventName()).isEqualTo("pull_request");
        assertThat(saved.getAction()).isEqualTo("review_requested");
        assertThat(saved.getGithubRepositoryId()).isEqualTo(12345L);
        assertThat(saved.getProcessingStatus()).isEqualTo("PENDING");
    }

    @Test
    void 잘못된_서명은_거부한다() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.receive(
                "delivery-1",
                "ping",
                "sha256=invalid",
                payload
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("서명이 올바르지 않습니다");
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 같은_delivery는_중복_저장하지_않는다() throws Exception {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        when(repository.existsById("delivery-1")).thenReturn(true);

        service.receive("delivery-1", "ping", signature(payload), payload);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void installation이_삭제되면_팀_연결도_삭제한다() throws Exception {
        byte[] payload = """
                {
                  "action": "deleted",
                  "installation": {"id": 500}
                }
                """.getBytes(StandardCharsets.UTF_8);
        when(repository.existsById("delivery-2")).thenReturn(false);

        service.receive("delivery-2", "installation", signature(payload), payload);

        verify(teamInstallationRepository).deleteAllByInstallationId(500L);
    }

    private String signature(byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }
}
