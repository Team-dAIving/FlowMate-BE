package kr.co.daeun.flowmatebe.domain.project.repository;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByTeam_IdInOrderByCreatedAtAsc(Collection<Long> teamIds);

    List<Project> findAllByTeam_IdOrderByCreatedAtAsc(Long teamId);
}
