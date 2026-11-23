package kr.co.daeun.flowmatebe.domain.github.repository;

import kr.co.daeun.flowmatebe.domain.github.entity.GithubWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GithubWebhookDeliveryRepository
        extends JpaRepository<GithubWebhookDelivery, String> {

    List<GithubWebhookDelivery> findAllByEventNameAndActionOrderByReceivedAtDesc(
            String eventName,
            String action
    );

    List<GithubWebhookDelivery>
    findTop20ByEventNameAndActionInAndProcessingStatusOrderByReceivedAtAsc(
            String eventName,
            List<String> actions,
            String processingStatus
    );
}
