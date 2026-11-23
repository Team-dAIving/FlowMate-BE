package kr.co.daeun.flowmatebe.domain.meeting.dto.response;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingResponseTest {

    @Test
    void 회의_상세에_분석_결과와_업무_제안을_포함한다() {
        Project project = new Project();
        project.setId(10L);
        Meeting meeting = new Meeting();
        meeting.setId(20L);
        meeting.setProject(project);
        meeting.setSummary("회의 요약");
        meeting.setTopics(List.of("API"));
        meeting.setDecisions(List.of(Map.of(
                "content", "API 구현",
                "evidenceSegmentIds", List.of("segment-1")
        )));
        meeting.setUnresolvedItems(List.of("배포일"));

        MeetingTaskSuggestion suggestion = new MeetingTaskSuggestion();
        suggestion.setId(40L);
        suggestion.setMeeting(meeting);
        suggestion.setSuggestionType("CREATE");
        suggestion.setProposedData(Map.of("title", "새 업무"));
        suggestion.setReviewStatus("PENDING");
        meeting.getTaskSuggestions().add(suggestion);

        MeetingResponse response = MeetingResponse.from(meeting);

        assertThat(response.analysis().summary()).isEqualTo("회의 요약");
        assertThat(response.analysis().decisions()).singleElement().satisfies(decision ->
                assertThat(decision).containsEntry("content", "API 구현")
        );
        assertThat(response.taskSuggestions()).singleElement().satisfies(value -> {
            assertThat(value.suggestionId()).isEqualTo(40L);
            assertThat(value.reviewStatus()).isEqualTo("PENDING");
            assertThat(value.proposedData()).containsEntry("title", "새 업무");
        });
    }
}
