package kr.co.daeun.flowmatebe.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "meeting_task_suggestions")
@Getter
@Setter
@NoArgsConstructor
public class MeetingTaskSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "suggestion_type", columnDefinition = "text")
    private String suggestionType;

    @Column(name = "target_task_id")
    private Long targetTaskId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_data")
    private Map<String, Object> proposedData;

    @Column(name = "review_status", columnDefinition = "text")
    private String reviewStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
