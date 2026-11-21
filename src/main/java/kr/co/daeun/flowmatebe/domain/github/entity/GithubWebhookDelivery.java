package kr.co.daeun.flowmatebe.domain.github.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "github_webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
public class GithubWebhookDelivery {

    @Id
    @Column(name = "delivery_id", columnDefinition = "text")
    private String deliveryId;

    @Column(name = "event_name", columnDefinition = "text")
    private String eventName;

    @Column(columnDefinition = "text")
    private String action;

    @Column(name = "github_repository_id")
    private Long githubRepositoryId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(name = "processing_status", columnDefinition = "text")
    private String processingStatus;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
