package kr.co.daeun.flowmatebe.domain.meeting.repository;

import kr.co.daeun.flowmatebe.domain.meeting.entity.MeetingTaskSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingTaskSuggestionRepository
        extends JpaRepository<MeetingTaskSuggestion, Long> {

    void deleteAllByMeeting_Id(Long meetingId);

    @Query("""
            select mts.meeting.id as meetingId, count(mts.id) as suggestionCount
            from MeetingTaskSuggestion mts
            where mts.meeting.id in :meetingIds
              and upper(mts.reviewStatus) = upper(:reviewStatus)
            group by mts.meeting.id
            """)
    List<MeetingSuggestionCount> countByMeetingIdsAndReviewStatus(
            @Param("meetingIds") List<Long> meetingIds,
            @Param("reviewStatus") String reviewStatus
    );

    interface MeetingSuggestionCount {

        Long getMeetingId();

        long getSuggestionCount();
    }
}
