package kr.co.daeun.flowmatebe.domain.project.repository;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByTeam_IdInOrderByCreatedAtAsc(Collection<Long> teamIds);

    List<Project> findAllByTeam_IdOrderByCreatedAtAsc(Long teamId);

    @Query("""
            select pm.project
            from ProjectMember pm
            join fetch pm.project.team
            where pm.user.id = :userId
            order by pm.project.createdAt desc, pm.project.id desc
            """)
    List<Project> findAllByMemberUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
            select p
            from Project p
            join fetch p.team
            where p.id = :projectId
              and exists (
                  select pm.id
                  from ProjectMember pm
                  where pm.project = p
                    and pm.user.id = :userId
              )
            """)
    Optional<Project> findAccessibleByIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );
}
