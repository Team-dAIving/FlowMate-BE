package kr.co.daeun.flowmatebe.domain.github.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team_github_installations")
@Getter
@Setter
@NoArgsConstructor
public class TeamGithubInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Column(name = "installation_id", nullable = false)
    private Long installationId;

    @Column(name = "github_account_id", nullable = false)
    private Long githubAccountId;

    @Column(name = "account_login", nullable = false, columnDefinition = "text")
    private String accountLogin;
}
