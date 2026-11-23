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

    @Query("""
            select t.project.id as projectId,
                   count(t.id) as totalCount,
                   sum(case when upper(t.status) = upper(:completedStatus) then 1 else 0 end)
                       as completedCount
            from Task t
            where t.project.id in :projectIds
            group by t.project.id
            """)
    List<ProjectTaskCount> countTasksByProjectIds(
            @Param("projectIds") Collection<Long> projectIds,
            @Param("completedStatus") String completedStatus
    );

    @Query("""
            select t
            from Task t
            left join fetch t.assigneeMember am
            left join fetch am.user
            left join fetch t.githubRepository
            where t.project.id = :projectId
            order by t.createdAt desc, t.id desc
            """)
    List<Task> findAllWithDetailsByProjectId(@Param("projectId") Long projectId);

    @Query("""
            select t
            from Task t
            left join fetch t.assigneeMember am
            left join fetch am.user
            left join fetch t.githubRepository
            where t.project.id in :projectIds
            order by t.project.createdAt desc, t.project.id desc, t.createdAt desc, t.id desc
            """)
    List<Task> findAllWithDetailsByProjectIds(
            @Param("projectIds") Collection<Long> projectIds
    );

    interface TeamTaskCount {

        Long getTeamId();

        long getTaskCount();
    }

    interface ProjectTaskCount {

        Long getProjectId();

        long getTotalCount();

        long getCompletedCount();
    }
}
