package kr.co.daeun.flowmatebe.domain.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "project_repositories")
@Getter
@Setter
@NoArgsConstructor
public class ProjectRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "github_repository_id", nullable = false, unique = true)
    private Long githubRepositoryId;

    @Column(name = "github_installation_id")
    private Long githubInstallationId;

    @Column(name = "owner_login", columnDefinition = "text")
    private String ownerLogin;

    @Column(name = "repository_name", columnDefinition = "text")
    private String repositoryName;

    @Column(name = "html_url", columnDefinition = "text")
    private String htmlUrl;

    @CreationTimestamp
    @Column(name = "connected_at", updatable = false)
    private Instant connectedAt;
}
