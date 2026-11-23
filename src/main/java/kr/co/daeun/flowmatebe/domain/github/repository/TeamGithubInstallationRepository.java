package kr.co.daeun.flowmatebe.domain.github.repository;

import kr.co.daeun.flowmatebe.domain.github.entity.TeamGithubInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamGithubInstallationRepository
        extends JpaRepository<TeamGithubInstallation, Long> {

    Optional<TeamGithubInstallation> findByTeam_Id(Long teamId);

    void deleteAllByInstallationId(Long installationId);
}
