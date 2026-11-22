package kr.co.daeun.flowmatebe.domain.project.repository;

import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepositoryLinkRepository extends JpaRepository<ProjectRepository, Long> {

    boolean existsByGithubRepositoryId(Long githubRepositoryId);
}
