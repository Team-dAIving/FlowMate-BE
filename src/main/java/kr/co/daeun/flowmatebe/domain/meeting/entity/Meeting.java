package kr.co.daeun.flowmatebe.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(columnDefinition = "text")
    private String title;

    @Column(name = "held_at")
    private Instant heldAt;

    @Column(name = "recording_s3_key", columnDefinition = "text")
    private String recordingS3Key;

    @Column(columnDefinition = "text")
    private String transcript;

    @Column(name = "transcription_job_id", length = 255)
    private String transcriptionJobId;

    @Column(name = "transcript_language", length = 20)
    private String transcriptLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transcript_segments")
    private List<Map<String, Object>> transcriptSegments = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> decisions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> topics = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unresolved_items")
    private List<String> unresolvedItems = new ArrayList<>();

    @Column(name = "processing_status", columnDefinition = "text")
    private String processingStatus;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<MeetingTaskSuggestion> taskSuggestions = new ArrayList<>();
}
