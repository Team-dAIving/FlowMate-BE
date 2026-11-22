package kr.co.daeun.flowmatebe.domain.meeting.repository;

import kr.co.daeun.flowmatebe.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    long countByProject_Id(Long projectId);

    List<Meeting> findAllByProject_IdOrderByHeldAtDescIdDesc(Long projectId);
}
