package kr.co.daeun.flowmatebe.domain.project.service;

import kr.co.daeun.flowmatebe.domain.project.dto.request.GenerateIssueDraftsRequest;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamMemberRepository;
import kr.co.daeun.flowmatebe.domain.team.repository.TeamRepository;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import kr.co.daeun.flowmatebe.domain.user.repository.UserRepository;
import kr.co.daeun.flowmatebe.global.ai.client.AiIssueClient;
import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateRequest;
import kr.co.daeun.flowmatebe.global.ai.dto.AiIssueGenerateResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectIssueDraftServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
    private final AiIssueClient aiIssueClient = mock(AiIssueClient.class);
    private final ProjectIssueDraftService service = new ProjectIssueDraftService(
            userRepository,
            teamRepository,
            teamMemberRepository,
            aiIssueClient
    );

    @Test
    void 멤버별_파트와_기술스택을_AI_요청으로_변환한다() {
        User leader = user(1L, "이다은");
        User member = user(2L, "김개발");
        when(userRepository.findAllById(any())).thenReturn(List.of(leader, member));
        when(aiIssueClient.generateIssues(any())).thenReturn(new AiIssueGenerateResponse(List.of(
                new AiIssueGenerateResponse.Issue(
                        "로그인 API 구현",
                        "JWT 로그인을 구현합니다.",
                        "HIGH",
                        1L,
                        LocalDate.of(2026, 11, 22),
                        LocalDate.of(2026, 11, 25),
                        8.0,
                        null
                )
        )));

        var response = service.generate(1L, validRequest());

        ArgumentCaptor<AiIssueGenerateRequest> captor = ArgumentCaptor.forClass(
                AiIssueGenerateRequest.class
        );
        verify(aiIssueClient).generateIssues(captor.capture());
        AiIssueGenerateRequest aiRequest = captor.getValue();
        assertThat(aiRequest.project().techStack())
                .containsExactly("Spring Boot", "Java", "React", "TypeScript");
        assertThat(aiRequest.members())
                .extracting(AiIssueGenerateRequest.Member::role)
                .containsExactly("BACKEND", "FRONTEND");
        assertThat(aiRequest.planText())
                .contains("프로젝트 리더: 이다은")
                .contains("projectRole=Leader")
                .doesNotContain("github.com/team/backend");
        assertThat(response.issues()).hasSize(1);
        assertThat(response.issues().get(0).labels()).isEmpty();
        verifyNoInteractions(teamRepository, teamMemberRepository);
    }

    @Test
    void AI가_프로젝트_멤버가_아닌_담당자를_반환하면_거절한다() {
        when(userRepository.findAllById(any())).thenReturn(List.of(
                user(1L, "이다은"),
                user(2L, "김개발")
        ));
        when(aiIssueClient.generateIssues(any())).thenReturn(new AiIssueGenerateResponse(List.of(
                new AiIssueGenerateResponse.Issue(
                        "업무",
                        "설명",
                        "MEDIUM",
                        999L,
                        LocalDate.of(2026, 11, 22),
                        LocalDate.of(2026, 11, 23),
                        null,
                        List.of()
                )
        )));

        assertThatThrownBy(() -> service.generate(1L, validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502 BAD_GATEWAY");
    }

    @Test
    void 리더가_프로젝트_멤버가_아니면_AI를_호출하지_않는다() {
        GenerateIssueDraftsRequest request = new GenerateIssueDraftsRequest(
                "FlowMate",
                "AI 협업 서비스",
                "MVP 출시",
                "WEB",
                LocalDate.of(2026, 11, 22),
                LocalDate.of(2026, 12, 31),
                null,
                99L,
                validRequest().members()
        );

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("프로젝트 리더는 프로젝트 멤버 중에서 선택해야 합니다.");
        verifyNoInteractions(userRepository, aiIssueClient);
    }

    private GenerateIssueDraftsRequest validRequest() {
        return new GenerateIssueDraftsRequest(
                "FlowMate",
                "AI 협업 서비스",
                "MVP 출시",
                "WEB",
                LocalDate.of(2026, 11, 22),
                LocalDate.of(2026, 12, 31),
                null,
                1L,
                List.of(
                        new GenerateIssueDraftsRequest.Member(
                                1L,
                                "backend",
                                "Spring Boot, Java",
                                "https://github.com/team/backend"
                        ),
                        new GenerateIssueDraftsRequest.Member(
                                2L,
                                "frontend",
                                "React, TypeScript",
                                null
                        )
                )
        );
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }
}
