package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.request.CreateProjectRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.response.CreateProjectResponse;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepositoryLinkRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamMemberRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.global.github.dto.GithubIssueMetadata;
import kr.co.daeun.flowmatebe.global.github.dto.GithubRepositorySnapshot;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectCreationService {

    private static final String PROJECT_STATUS = "IN_PROGRESS";
    private static final String LEADER_ROLE = "Leader";
    private static final String MEMBER_ROLE = "Member";

    private final kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepositoryLinkRepository repositoryLinkRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final GithubInstallationClient githubInstallationClient;

    @Transactional
    public CreateProjectResponse create(Long currentUserId, CreateProjectRequest request) {
        validateDates(request);
        Team team = teamRepository.findByIdAndMemberUserId(request.teamId(), currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "프로젝트를 생성할 팀을 찾을 수 없습니다."
                ));

        Map<Long, User> teamUsers = teamMemberRepository
                .findAllWithUserByTeamId(team.getId())
                .stream()
                .map(TeamMember::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, CreateProjectRequest.Member> requestedMembers = indexMembers(request);
        validateMembers(currentUserId, request.leaderUserId(), requestedMembers, teamUsers);

        List<RepositoryConnection> repositoryConnections = validateRepositories(request);

        Project project = new Project();
        project.setName(request.name().strip());
        project.setDescription(request.description().strip());
        project.setSubject(request.goal().strip());
        project.setType(request.type().strip().toUpperCase(Locale.ROOT));
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setStatus(PROJECT_STATUS);
        project.setTeam(team);
        Map<Long, String> repositoryRoles = repositoryConnections.stream()
                .collect(Collectors.toMap(
                        connection -> connection.snapshot().repository().githubRepositoryId(),
                        RepositoryConnection::role,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        Map<String, Object> projectTechStack = new LinkedHashMap<>();
        projectTechStack.put("additional", normalizeTechnologies(request.additionalTechStack()));
        projectTechStack.put("repositoryRoles", repositoryRoles);
        project.setTechStack(projectTechStack);
        Project savedProject = projectRepository.save(project);

        List<ProjectMember> projectMembers = requestedMembers.values().stream()
                .map(member -> toProjectMember(
                        savedProject,
                        teamUsers.get(member.userId()),
                        member,
                        request.leaderUserId()
                ))
                .toList();
        projectMemberRepository.saveAll(projectMembers);

        List<kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository> links =
                repositoryConnections.stream()
                        .map(connection -> toRepositoryLink(
                                savedProject,
                                connection.snapshot().repository()
                        ))
                        .toList();
        List<kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository> savedLinks =
                repositoryLinkRepository.saveAll(links);

        Map<Long, kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository> linksByRepositoryId =
                savedLinks.stream().collect(Collectors.toMap(
                        kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository::getGithubRepositoryId,
                        Function.identity()
                ));
        Map<String, ProjectMember> membersByGithubLogin = projectMembers.stream()
                .filter(member -> member.getUser().getGithubLogin() != null)
                .collect(Collectors.toMap(
                        member -> member.getUser().getGithubLogin().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (first, ignored) -> first
                ));
        List<Task> importedTasks = repositoryConnections.stream()
                .flatMap(connection -> connection.snapshot().issues().stream()
                        .map(issue -> toTask(
                                savedProject,
                                linksByRepositoryId.get(
                                        connection.snapshot().repository().githubRepositoryId()
                                ),
                                membersByGithubLogin,
                                issue
                        )))
                .toList();
        taskRepository.saveAll(importedTasks);

        String issueImportStatus = repositoryConnections.isEmpty()
                ? "NOT_REQUESTED"
                : repositoryConnections.stream()
                .allMatch(connection -> connection.snapshot().issueImportSucceeded())
                ? "SUCCESS"
                : "FAILED";
        return CreateProjectResponse.of(
                savedProject,
                savedLinks,
                repositoryRoles,
                importedTasks.size(),
                issueImportStatus
        );
    }

    private void validateDates(CreateProjectRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "종료 예정일은 시작일보다 빠를 수 없습니다."
            );
        }
    }

    private Map<Long, CreateProjectRequest.Member> indexMembers(CreateProjectRequest request) {
        Map<Long, CreateProjectRequest.Member> members = new LinkedHashMap<>();
        for (CreateProjectRequest.Member member : request.members()) {
            if (members.putIfAbsent(member.userId(), member) != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "중복된 프로젝트 멤버가 포함되어 있습니다: " + member.userId()
                );
            }
        }
        return members;
    }

    private void validateMembers(
            Long currentUserId,
            Long leaderUserId,
            Map<Long, CreateProjectRequest.Member> requestedMembers,
            Map<Long, User> teamUsers
    ) {
        if (!requestedMembers.containsKey(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "프로젝트 생성자는 프로젝트 멤버에 포함되어야 합니다."
            );
        }
        if (!requestedMembers.containsKey(leaderUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "프로젝트 리더는 프로젝트 멤버에 포함되어야 합니다."
            );
        }
        List<Long> nonTeamMemberIds = requestedMembers.keySet().stream()
                .filter(userId -> !teamUsers.containsKey(userId))
                .toList();
        if (!nonTeamMemberIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "팀에 속하지 않은 사용자가 포함되어 있습니다: " + nonTeamMemberIds
            );
        }
    }

    private List<RepositoryConnection> validateRepositories(CreateProjectRequest request) {
        Set<Long> repositoryIds = new LinkedHashSet<>();
        List<RepositoryConnection> connections = new ArrayList<>();
        for (CreateProjectRequest.Repository repository : request.repositories()) {
            if (!repositoryIds.add(repository.githubRepositoryId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "중복된 GitHub 저장소가 포함되어 있습니다: "
                                + repository.githubRepositoryId()
                );
            }
            if (repositoryLinkRepository.existsByGithubRepositoryId(
                    repository.githubRepositoryId()
            )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 다른 프로젝트에 연결된 GitHub 저장소입니다: "
                                + repository.githubRepositoryId()
                );
            }
            GithubRepositorySnapshot snapshot = githubInstallationClient.getRepositorySnapshot(
                    repository.installationId(),
                    repository.githubRepositoryId()
            );
            connections.add(new RepositoryConnection(repository.role(), snapshot));
        }
        return connections;
    }

    private ProjectMember toProjectMember(
            Project project,
            User user,
            CreateProjectRequest.Member request,
            Long leaderUserId
    ) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(user.getId().equals(leaderUserId) ? LEADER_ROLE : MEMBER_ROLE);
        member.setParts(List.of(request.part()));
        member.setTechStack(normalizeTechnologies(request.techStack()));
        return member;
    }

    private kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository toRepositoryLink(
            Project project,
            kr.co.daeun.flowmatebe.global.github.dto.GithubRepositoryMetadata metadata
    ) {
        kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository link =
                new kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository();
        link.setProject(project);
        link.setGithubRepositoryId(metadata.githubRepositoryId());
        link.setGithubInstallationId(metadata.installationId());
        link.setOwnerLogin(metadata.ownerLogin());
        link.setRepositoryName(metadata.repositoryName());
        link.setHtmlUrl(metadata.htmlUrl());
        return link;
    }

    private Task toTask(
            Project project,
            kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository repository,
            Map<String, ProjectMember> membersByGithubLogin,
            GithubIssueMetadata issue
    ) {
        Task task = new Task();
        task.setProject(project);
        task.setGithubRepository(repository);
        task.setGithubIssueNumber(issue.number());
        task.setGithubSyncStatus("SYNCED");
        task.setTitle(issue.title());
        task.setDescription(issue.body());
        task.setStatus("closed".equalsIgnoreCase(issue.state()) ? "DONE" : "TODO");
        task.setPlanningStatus("PLANNED");
        task.setCompletedAt(issue.closedAt());
        if (issue.assigneeLogin() != null) {
            task.setAssigneeMember(membersByGithubLogin.get(
                    issue.assigneeLogin().toLowerCase(Locale.ROOT)
            ));
        }
        return task;
    }

    private List<String> normalizeTechnologies(List<String> technologies) {
        Map<String, String> normalized = new LinkedHashMap<>();
        technologies.stream()
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .forEach(value -> normalized.putIfAbsent(value.toLowerCase(Locale.ROOT), value));
        return List.copyOf(normalized.values());
    }

    private record RepositoryConnection(
            String role,
            GithubRepositorySnapshot snapshot
    ) {
    }
}
