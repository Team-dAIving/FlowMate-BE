package kr.co.daeun.flowmatebe.domain.github.service;

import kr.co.daeun.flowmatebe.domain.github.entity.GithubWebhookDelivery;
import kr.co.daeun.flowmatebe.domain.github.repository.GithubWebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GithubIssueWebhookPoller {

    private final GithubWebhookDeliveryRepository deliveryRepository;
    private final GithubWebhookService githubWebhookService;

    @Scheduled(fixedDelayString = "${app.github.webhook-poll-interval:PT5S}")
    public void poll() {
        List<String> deliveryIds = deliveryRepository
                .findTop20ByEventNameAndActionInAndProcessingStatusOrderByReceivedAtAsc(
                        "issues",
                        List.of("closed", "reopened"),
                        "PENDING"
                )
                .stream()
                .map(GithubWebhookDelivery::getDeliveryId)
                .toList();
        for (String deliveryId : deliveryIds) {
            try {
                githubWebhookService.processPendingIssueDelivery(deliveryId);
            } catch (RuntimeException exception) {
                log.warn(
                        "GitHub 이슈 webhook 처리 중 예기치 않은 오류가 발생했습니다. deliveryId={}",
                        deliveryId,
                        exception
                );
            }
        }
    }
}
