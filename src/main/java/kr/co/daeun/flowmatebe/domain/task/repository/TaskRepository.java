package kr.co.daeun.flowmatebe.domain.task.repository;

import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    @Query("""
            select distinct t
            from Task t
            join fetch t.project
            left join fetch t.assigneeMember am
            left join fetch am.user
            where t.project.team.id = :teamId
            order by t.id
            """)
    List<Task> findAllWithDetailsByTeamId(@Param("teamId") Long teamId);

    @Query("""
            select t
            from Task t
            join fetch t.project p
            left join fetch t.assigneeMember am
            left join fetch am.user
            left join fetch t.githubRepository
            where t.id = :taskId
              and exists (
                  select tm.id
                  from TeamMember tm
                  where tm.team = p.team
                    and tm.user.id = :userId
              )
            """)
    Optional<Task> findAccessibleDetailByIdAndUserId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

    interface TeamTaskCount {

        Long getTeamId();

        long getTaskCount();
    }
}
