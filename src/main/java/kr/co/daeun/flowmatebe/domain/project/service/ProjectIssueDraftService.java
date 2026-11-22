package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.request.GenerateIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.project.dto.response.GenerateIssueDraftsResponse;
import kr.co.daeun.flowmatebe.domain.team.entity.TeamMember;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamMemberRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import kr.co.daeun.flowmatebe.global.ai.client.AiIssueClient;
import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateRequest;
import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
public class ProjectIssueDraftService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AiIssueClient aiIssueClient;

    public GenerateIssueDraftsResponse generate(
            Long currentUserId,
            GenerateIssueDraftsRequest request
    ) {
        validateProjectDates(request);
        Map<Long, GenerateIssueDraftsRequest.Member> requestMembers = indexRequestMembers(request);
        validateLeaderAndCreator(currentUserId, request.leaderUserId(), requestMembers.keySet());
        Map<Long, User> usersById = loadUsers(requestMembers.keySet());
        validateTeamMembers(currentUserId, request.teamId(), requestMembers.keySet());

        AiIssueGenerateRequest aiRequest = toAiRequest(request, requestMembers, usersById);
        AiIssueGenerateResponse aiResponse = aiIssueClient.generateIssues(aiRequest);
        validateAiResponse(aiResponse, requestMembers.keySet(), request.startDate(), request.endDate());
        return GenerateIssueDraftsResponse.from(aiResponse);
    }

    private void validateProjectDates(GenerateIssueDraftsRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "프로젝트 시작일은 종료 예정일보다 늦을 수 없습니다."
            );
        }
    }

    private Map<Long, GenerateIssueDraftsRequest.Member> indexRequestMembers(
            GenerateIssueDraftsRequest request
    ) {
        Map<Long, GenerateIssueDraftsRequest.Member> membersById = new LinkedHashMap<>();
        for (GenerateIssueDraftsRequest.Member member : request.members()) {
            if (membersById.putIfAbsent(member.userId(), member) != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "중복된 프로젝트 멤버가 포함되어 있습니다: " + member.userId()
                );
            }
        }
        return membersById;
    }

    private void validateLeaderAndCreator(
            Long currentUserId,
            Long leaderUserId,
            Set<Long> memberUserIds
    ) {
        if (!memberUserIds.contains(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "프로젝트 생성자는 프로젝트 멤버에 포함되어야 합니다."
            );
        }
        if (!memberUserIds.contains(leaderUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "프로젝트 리더는 프로젝트 멤버 중에서 선택해야 합니다."
            );
        }
    }

    private Map<Long, User> loadUsers(Set<Long> memberUserIds) {
        Map<Long, User> usersById = userRepository.findAllById(memberUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> missingUserIds = memberUserIds.stream()
                .filter(userId -> !usersById.containsKey(userId))
                .toList();
        if (!missingUserIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 사용자 ID가 포함되어 있습니다: " + missingUserIds
            );
        }
        return usersById;
    }

    private void validateTeamMembers(
            Long currentUserId,
            Long teamId,
            Set<Long> requestedUserIds
    ) {
        if (teamId == null) {
            return;
        }
        teamRepository.findByIdAndMemberUserId(teamId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "팀을 찾을 수 없습니다."
                ));
        Set<Long> teamMemberUserIds = teamMemberRepository.findAllWithUserByTeamId(teamId).stream()
                .map(TeamMember::getUser)
                .map(User::getId)
                .collect(Collectors.toSet());
        List<Long> nonTeamMemberUserIds = requestedUserIds.stream()
                .filter(userId -> !teamMemberUserIds.contains(userId))
                .toList();
        if (!nonTeamMemberUserIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "선택한 팀에 속하지 않은 사용자가 포함되어 있습니다: " + nonTeamMemberUserIds
            );
        }
    }

    private AiIssueGenerateRequest toAiRequest(
            GenerateIssueDraftsRequest request,
            Map<Long, GenerateIssueDraftsRequest.Member> requestMembers,
            Map<Long, User> usersById
    ) {
        List<AiIssueGenerateRequest.Member> aiMembers = requestMembers.values().stream()
                .map(member -> new AiIssueGenerateRequest.Member(
                        member.userId(),
                        displayName(usersById.get(member.userId())),
                        member.part().toUpperCase(Locale.ROOT)
                ))
                .toList();
        List<String> projectTechStack = requestMembers.values().stream()
                .flatMap(member -> splitTechStack(member.techStack()).stream())
                .distinct()
                .toList();
        AiIssueGenerateRequest.Project project = new AiIssueGenerateRequest.Project(
                request.name().strip(),
                request.description().strip(),
                request.startDate(),
                request.endDate(),
                projectTechStack
        );
        return new AiIssueGenerateRequest(
                project,
                buildPlanText(request, requestMembers, usersById),
                aiMembers
        );
    }

    private String buildPlanText(
            GenerateIssueDraftsRequest request,
            Map<Long, GenerateIssueDraftsRequest.Member> requestMembers,
            Map<Long, User> usersById
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("프로젝트 설계 내용:");
        lines.add(request.designText().strip());
        lines.add("프로젝트 유형: " + request.type().strip());
        lines.add("프로젝트 리더: " + displayName(usersById.get(request.leaderUserId())));
        lines.add("프로젝트 멤버별 정보:");
        requestMembers.values().forEach(member -> {
            String projectRole = member.userId().equals(request.leaderUserId()) ? "Leader" : "Member";
            lines.add("- " + displayName(usersById.get(member.userId()))
                    + " (userId=" + member.userId()
                    + ", projectRole=" + projectRole
                    + ", part=" + member.part()
                    + ", techStack=" + String.join(", ", splitTechStack(member.techStack()))
                    + ")");
        });
        return String.join("\n", lines);
    }

    private List<String> splitTechStack(String value) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String normalized = item.strip();
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        if (values.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "멤버별 기술 스택을 한 개 이상 입력해야 합니다."
            );
        }
        return List.copyOf(values);
    }

    private String displayName(User user) {
        String nickname = normalizeNullable(user.getNickname());
        if (nickname != null) {
            return nickname;
        }
        String githubLogin = normalizeNullable(user.getGithubLogin());
        return githubLogin == null ? "사용자 " + user.getId() : githubLogin;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private void validateAiResponse(
            AiIssueGenerateResponse response,
            Set<Long> memberUserIds,
            java.time.LocalDate projectStartDate,
            java.time.LocalDate projectEndDate
    ) {
        Set<String> titles = new LinkedHashSet<>();
        for (AiIssueGenerateResponse.Issue issue : response.issues()) {
            if (issue == null || issue.title() == null || issue.title().isBlank()
                    || issue.description() == null || issue.priority() == null
                    || issue.startDate() == null || issue.dueDate() == null) {
                throw invalidAiResponse();
            }
            if (!Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(issue.priority())) {
                throw invalidAiResponse();
            }
            if (issue.assigneeId() != null && !memberUserIds.contains(issue.assigneeId())) {
                throw invalidAiResponse();
            }
            if (issue.startDate().isBefore(projectStartDate)
                    || issue.dueDate().isAfter(projectEndDate)
                    || issue.startDate().isAfter(issue.dueDate())) {
                throw invalidAiResponse();
            }
            if (!titles.add(issue.title().strip())) {
                throw invalidAiResponse();
            }
        }
    }

    private ResponseStatusException invalidAiResponse() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI 서버가 유효하지 않은 업무 초안을 반환했습니다."
        );
    }
}
