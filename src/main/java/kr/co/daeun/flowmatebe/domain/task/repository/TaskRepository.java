package kr.co.daeun.flowmatebe.domain.task.repository;

import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            select t.project.team.id as teamId, count(t.id) as taskCount
            from Task t
            where t.project.team.id in :teamIds
              and upper(t.status) = upper(:status)
            group by t.project.team.id
            """)
    List<TeamTaskCount> countTasksByTeamIdsAndStatus(
            @Param("teamIds") Collection<Long> teamIds,
            @Param("status") String status
    );

    interface TeamTaskCount {

        Long getTeamId();

        long getTaskCount();
    }
}
