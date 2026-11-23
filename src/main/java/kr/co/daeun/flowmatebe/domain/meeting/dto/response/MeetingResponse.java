package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MeetingResponse(
        Long meetingId,
        Long projectId,
        String title,
        Instant heldAt,
        Long durationSeconds,
        String processingStatus,
        String failureReason,
        String transcriptionJobId,
        List<Participant> participants,
        Transcript transcript,
        Analysis analysis,
        List<TaskSuggestion> taskSuggestions
) {

    public static MeetingResponse from(Meeting meeting) {
        return from(meeting, List.of());
    }

    public static MeetingResponse from(
            Meeting meeting,
            List<ProjectMember> projectMembers
    ) {
        List<Segment> segments = meeting.getTranscriptSegments() == null
                ? List.of()
                : meeting.getTranscriptSegments().stream()
                        .map(Segment::from)
                        .toList();
        Transcript transcript = meeting.getTranscript() == null
                ? null
                : new Transcript(
                        meeting.getTranscriptLanguage(),
                        meeting.getTranscript(),
                        segments
                );
        Analysis analysis = meeting.getSummary() == null
                ? null
                : new Analysis(
                        meeting.getSummary(),
                        copyList(meeting.getTopics()),
                        copyMapList(meeting.getDecisions()),
                        copyList(meeting.getUnresolvedItems())
                );
        List<Participant> participants = projectMembers.stream()
                .map(Participant::from)
                .toList();
        Map<Long, Participant> participantsByUserId = participants.stream()
                .collect(Collectors.toMap(
                        Participant::userId,
                        Function.identity()
                ));
        Map<String, Segment> segmentsById = segments.stream()
                .collect(Collectors.toMap(
                        Segment::segmentId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return new MeetingResponse(
                meeting.getId(),
                meeting.getProject().getId(),
                meeting.getTitle(),
                meeting.getHeldAt(),
                meeting.getDurationSeconds(),
                meeting.getProcessingStatus(),
                meeting.getFailureReason(),
                meeting.getTranscriptionJobId(),
                participants,
                transcript,
                analysis,
                meeting.getTaskSuggestions() == null
                        ? List.of()
                        : meeting.getTaskSuggestions().stream()
                                .map(suggestion -> TaskSuggestion.from(
                                        suggestion,
                                        participantsByUserId,
                                        segmentsById
                                ))
                                .toList()
        );
    }

    private static List<String> copyList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<Map<String, Object>> copyMapList(
            List<Map<String, Object>> values
    ) {
        return values == null
                ? List.of()
                : values.stream()
                        .<Map<String, Object>>map(LinkedHashMap::new)
                        .toList();
    }

    public record Participant(
            Long userId,
            String nickname,
            String avatarUrl,
            String role,
            List<String> parts
    ) {

        private static Participant from(ProjectMember member) {
            return new Participant(
                    member.getUser().getId(),
                    member.getUser().getNickname(),
                    member.getUser().getAvatarUrl(),
                    member.getRole(),
                    member.getParts() == null ? List.of() : List.copyOf(member.getParts())
            );
        }
    }

    public record Transcript(
            String language,
            String fullText,
            List<Segment> segments
    ) {
    }

    public record Segment(
            String segmentId,
            String speakerId,
            Long startMs,
            Long endMs,
            String timestamp,
            String text
    ) {

        private static Segment from(Map<String, Object> value) {
            Long startMs = number(value.get("startMs"));
            return new Segment(
                    string(value.get("segmentId")),
                    nullableString(value.get("speakerId")),
                    startMs,
                    number(value.get("endMs")),
                    formatTimestamp(startMs),
                    string(value.get("text"))
            );
        }

        private static String formatTimestamp(Long milliseconds) {
            if (milliseconds == null) {
                return null;
            }
            long totalSeconds = milliseconds / 1000;
            long hours = totalSeconds / 3600;
            long minutes = totalSeconds % 3600 / 60;
            long seconds = totalSeconds % 60;
            return hours > 0
                    ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    : String.format("%02d:%02d", minutes, seconds);
        }

        private static Long number(Object value) {
            return value instanceof Number number ? number.longValue() : null;
        }

        private static String string(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private static String nullableString(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }

    public record Analysis(
            String summary,
            List<String> topics,
            List<Map<String, Object>> decisions,
            List<String> unresolvedItems
    ) {
    }

    public record TaskSuggestion(
            Long suggestionId,
            String suggestionType,
            Long targetTaskId,
            Map<String, Object> proposedData,
            String priority,
            Participant assignee,
            List<EvidenceSegment> evidenceSegments,
            String reviewStatus,
            Instant createdAt
    ) {

        private static TaskSuggestion from(
                MeetingTaskSuggestion suggestion,
                Map<Long, Participant> participantsByUserId,
                Map<String, Segment> segmentsById
        ) {
            Map<String, Object> proposedData = suggestion.getProposedData() == null
                    ? Map.of()
                    : suggestion.getProposedData();
            Long assigneeId = longValue(proposedData.get("assigneeId"));
            return new TaskSuggestion(
                    suggestion.getId(),
                    suggestion.getSuggestionType(),
                    suggestion.getTargetTaskId(),
                    proposedData,
                    priority(proposedData),
                    assigneeId == null ? null : participantsByUserId.get(assigneeId),
                    evidenceIds(proposedData).stream()
                            .map(segmentsById::get)
                            .filter(java.util.Objects::nonNull)
                            .map(EvidenceSegment::from)
                            .toList(),
                    suggestion.getReviewStatus(),
                    suggestion.getCreatedAt()
            );
        }

        private static String priority(Map<String, Object> proposedData) {
            Object priority = proposedData.get("priority");
            if (priority != null) {
                return String.valueOf(priority);
            }
            Object changes = proposedData.get("changes");
            if (changes instanceof Map<?, ?> map && map.get("priority") != null) {
                return String.valueOf(map.get("priority"));
            }
            return "MEDIUM";
        }

        private static Long longValue(Object value) {
            return value instanceof Number number ? number.longValue() : null;
        }

        private static List<String> evidenceIds(Map<String, Object> proposedData) {
            Object value = proposedData.get("evidenceSegmentIds");
            if (!(value instanceof List<?> list)) {
                return List.of();
            }
            return list.stream().map(String::valueOf).toList();
        }
    }

    public record EvidenceSegment(
            String segmentId,
            String timestamp,
            Long startMs,
            Long endMs,
            String text
    ) {

        private static EvidenceSegment from(Segment segment) {
            return new EvidenceSegment(
                    segment.segmentId(),
                    segment.timestamp(),
                    segment.startMs(),
                    segment.endMs(),
                    segment.text()
            );
        }
    }
}
