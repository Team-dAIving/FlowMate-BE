package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.request.RegisterIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.response.RegisterIssueDraftsResponse;
import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepositoryLinkRepository;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskRequest;
import kr.co.daeun.flowmatebe.domain.task.dto.response.CreateTaskResponse;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.global.github.dto.GithubCreatedIssue;
import kr.co.daeun.flowmatebe.global.github.service.GithubInstallationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectIssueRegistrationService {

    private static final String TASK_STATUS = "TODO";
    private static final String PLANNING_STATUS = "CONFIRMED";
    private static final String SYNC_PENDING = "PENDING";
    private static final String SYNCED = "SYNCED";
    private static final String SYNC_FAILED = "FAILED";
    private static final String SYNC_NOT_LINKED = "NOT_LINKED";
    private static final ZoneId PROJECT_ZONE = ZoneId.of("Asia/Seoul");

    private final kr.co.daeun.flowmatebe.domain.project.repository.ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepositoryLinkRepository repositoryLinkRepository;
    private final TaskRepository taskRepository;
    private final GithubInstallationClient githubInstallationClient;

    public RegisterIssueDraftsResponse register(
            Long currentUserId,
            Long projectId,
            RegisterIssueDraftsRequest request
    ) {
        return registerIssues(currentUserId, projectId, request.issues());
    }

    public CreateTaskResponse createManualTask(
            Long currentUserId,
            Long projectId,
            CreateTaskRequest request
    ) {
        RegisterIssueDraftsRequest.Issue issue = new RegisterIssueDraftsRequest.Issue(
                request.title(),
                request.description(),
                request.priority(),
                request.assigneeId(),
                LocalDate.now(PROJECT_ZONE),
                request.dueDate(),
                request.estimatedHours(),
                request.labels(),
                request.createGithubIssue(),
                request.githubRepositoryId()
        );
        RegisterIssueDraftsResponse response = registerIssues(
                currentUserId,
                projectId,
                List.of(issue)
        );
        return CreateTaskResponse.from(response.tasks().get(0));
    }

    private RegisterIssueDraftsResponse registerIssues(
            Long currentUserId,
            Long projectId,
            List<RegisterIssueDraftsRequest.Issue> issues
    ) {
        Project project = projectRepository.findAccessibleByIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "접근 가능한 프로젝트를 찾을 수 없습니다."
                ));
        Map<Long, ProjectMember> membersByUserId = projectMemberRepository
                .findAllWithUserByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(
                        member -> member.getUser().getId(),
                        Function.identity()
                ));
        List<ProjectRepository> repositories = repositoryLinkRepository
                .findAllByProject_IdOrderByIdAsc(projectId);
        Map<Long, ProjectRepository> repositoriesByGithubId = repositories.stream()
                .collect(Collectors.toMap(
                        ProjectRepository::getGithubRepositoryId,
                        Function.identity()
                ));

        List<Registration> registrations = issues.stream()
                .map(issue -> prepareRegistration(
                        project,
                        membersByUserId,
                        repositories,
                        repositoriesByGithubId,
                        issue
                ))
                .toList();

        // First persist every local task. GitHub is an external system and cannot participate
        // in the database transaction, so a remote failure is recorded on the task instead of
        // rolling back already-created GitHub issues or losing the local task.
        taskRepository.saveAll(registrations.stream().map(Registration::task).toList());

        List<SyncResult> syncResults = new ArrayList<>();
        for (Registration registration : registrations) {
            syncResults.add(createGithubIssue(registration));
        }
        taskRepository.saveAll(registrations.stream().map(Registration::task).toList());

        int createdGithubIssueCount = (int) syncResults.stream()
                .filter(result -> SYNCED.equals(result.registration().task().getGithubSyncStatus()))
                .count();
        int failedGithubIssueCount = (int) syncResults.stream()
                .filter(result -> SYNC_FAILED.equals(result.registration().task().getGithubSyncStatus()))
                .count();
        return new RegisterIssueDraftsResponse(
                registrations.size(),
                createdGithubIssueCount,
                failedGithubIssueCount,
                syncResults.stream().map(this::toResponse).toList()
        );
    }

    private Registration prepareRegistration(
            Project project,
            Map<Long, ProjectMember> membersByUserId,
            List<ProjectRepository> repositories,
            Map<Long, ProjectRepository> repositoriesByGithubId,
            RegisterIssueDraftsRequest.Issue issue
    ) {
        validateDates(project, issue);
        ProjectMember assignee = null;
        if (issue.assigneeId() != null) {
            assignee = membersByUserId.get(issue.assigneeId());
            if (assignee == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "프로젝트 멤버가 아닌 담당자가 포함되어 있습니다: " + issue.assigneeId()
                );
            }
        }

        ProjectRepository githubRepository = Boolean.TRUE.equals(issue.createGithubIssue())
                ? resolveGithubRepository(
                        issue.githubRepositoryId(),
                        assignee,
                        repositories,
                        repositoriesByGithubId
                )
                : null;

        Task task = new Task();
        task.setProject(project);
        task.setAssigneeMember(assignee);
        task.setTitle(issue.title().strip());
        task.setDescription(issue.description().strip());
        task.setStatus(TASK_STATUS);
        task.setPriority(issue.priority().strip().toUpperCase(Locale.ROOT));
        task.setPlanningStatus(PLANNING_STATUS);
        task.setStartDate(issue.startDate());
        task.setDueDate(issue.dueDate());
        task.setEstimatedTime(toEstimatedMinutes(issue.estimatedHours()));
        task.setGithubRepository(githubRepository);
        task.setGithubSyncStatus(Boolean.TRUE.equals(issue.createGithubIssue())
                ? SYNC_PENDING
                : SYNC_NOT_LINKED);

        return new Registration(task, issue, githubRepository, normalizeLabels(issue.labels()));
    }

    private void validateDates(Project project, RegisterIssueDraftsRequest.Issue issue) {
        if (issue.startDate().isAfter(issue.dueDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업무 시작일은 마감일보다 늦을 수 없습니다: " + issue.title()
            );
        }
        if (issue.startDate().isBefore(project.getStartDate())
                || issue.dueDate().isAfter(project.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업무 일정은 프로젝트 기간 안에 있어야 합니다: " + issue.title()
            );
        }
    }

    private ProjectRepository resolveGithubRepository(
            Long requestedGithubRepositoryId,
            ProjectMember assignee,
            List<ProjectRepository> repositories,
            Map<Long, ProjectRepository> repositoriesByGithubId
    ) {
        if (requestedGithubRepositoryId != null) {
            ProjectRepository requested = repositoriesByGithubId.get(requestedGithubRepositoryId);
            if (requested == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "프로젝트에 연결되지 않은 GitHub 저장소입니다: "
                                + requestedGithubRepositoryId
                );
            }
            return requested;
        }

        if (assignee != null && assignee.getParts() != null) {
            Set<String> assigneeParts = assignee.getParts().stream()
                    .filter(Objects::nonNull)
                    .map(value -> value.strip().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            ProjectRepository partRepository = repositories.stream()
                    .filter(repository -> repository.getRepositoryRole() != null)
                    .filter(repository -> assigneeParts.contains(
                            repository.getRepositoryRole().strip().toUpperCase(Locale.ROOT)
                    ))
                    .findFirst()
                    .orElse(null);
            if (partRepository != null) {
                return partRepository;
            }
        }

        ProjectRepository commonRepository = repositories.stream()
                .filter(repository -> "COMMON".equalsIgnoreCase(repository.getRepositoryRole()))
                .findFirst()
                .orElse(null);
        if (commonRepository != null) {
            return commonRepository;
        }
        if (repositories.size() == 1) {
            return repositories.get(0);
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                repositories.isEmpty()
                        ? "GitHub 이슈를 생성하려면 프로젝트에 저장소를 연결해야 합니다."
                        : "GitHub 이슈를 생성할 저장소를 결정할 수 없습니다. githubRepositoryId를 지정해 주세요."
        );
    }

    private SyncResult createGithubIssue(Registration registration) {
        if (!Boolean.TRUE.equals(registration.request().createGithubIssue())) {
            return new SyncResult(registration, null, null);
        }
        ProjectRepository repository = registration.repository();
        try {
            validateRepositoryMetadata(repository);
            GithubCreatedIssue created = githubInstallationClient.createIssue(
                    repository.getGithubInstallationId(),
                    repository.getOwnerLogin(),
                    repository.getRepositoryName(),
                    registration.task().getTitle(),
                    registration.task().getDescription(),
                    registration.labels()
            );
            registration.task().setGithubIssueNumber(created.number());
            registration.task().setGithubSyncStatus(SYNCED);
            return new SyncResult(registration, created.htmlUrl(), null);
        } catch (ResponseStatusException exception) {
            registration.task().setGithubSyncStatus(SYNC_FAILED);
            return new SyncResult(
                    registration,
                    null,
                    exception.getReason() == null
                            ? "GitHub 이슈 생성에 실패했습니다."
                            : exception.getReason()
            );
        }
    }

    private void validateRepositoryMetadata(ProjectRepository repository) {
        if (repository.getGithubInstallationId() == null
                || repository.getOwnerLogin() == null
                || repository.getOwnerLogin().isBlank()
                || repository.getRepositoryName() == null
                || repository.getRepositoryName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "연결된 GitHub 저장소 정보가 올바르지 않습니다."
            );
        }
    }

    private Integer toEstimatedMinutes(Double estimatedHours) {
        if (estimatedHours == null) {
            return null;
        }
        return (int) Math.round(estimatedHours * 60.0);
    }

    private List<String> normalizeLabels(List<String> labels) {
        Map<String, String> normalized = new LinkedHashMap<>();
        labels.stream()
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .forEach(value -> normalized.putIfAbsent(value.toLowerCase(Locale.ROOT), value));
        return List.copyOf(normalized.values());
    }

    private RegisterIssueDraftsResponse.TaskResult toResponse(SyncResult result) {
        Task task = result.registration().task();
        ProjectRepository repository = result.registration().repository();
        return new RegisterIssueDraftsResponse.TaskResult(
                task.getId(),
                task.getTitle(),
                Boolean.TRUE.equals(result.registration().request().createGithubIssue()),
                task.getGithubSyncStatus(),
                repository == null ? null : repository.getGithubRepositoryId(),
                task.getGithubIssueNumber(),
                result.githubIssueUrl(),
                result.error()
        );
    }

    private record Registration(
            Task task,
            RegisterIssueDraftsRequest.Issue request,
            ProjectRepository repository,
            List<String> labels
    ) {
    }

    private record SyncResult(
            Registration registration,
            String githubIssueUrl,
            String error
    ) {
    }
}
