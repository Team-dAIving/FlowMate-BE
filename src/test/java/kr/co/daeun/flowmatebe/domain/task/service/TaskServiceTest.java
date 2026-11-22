package kr.co.daeun.flowmatebe.domain.task.service;

import kr.co.daeun.flowmatebe.domain.project.entity.Project;
import kr.co.daeun.flowmatebe.domain.project.entity.ProjectMember;
import kr.co.daeun.flowmatebe.domain.project.repository.ProjectMemberRepository;
import kr.co.daeun.flowmatebe.domain.task.dto.request.CreateTaskCommentRequest;
import kr.co.daeun.flowmatebe.domain.task.entity.Task;
import kr.co.daeun.flowmatebe.domain.task.entity.TaskComment;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskCommentRepository;
import kr.co.daeun.flowmatebe.domain.task.repository.TaskRepository;
import kr.co.daeun.flowmatebe.domain.team.entity.Team;
import kr.co.daeun.flowmatebe.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskCommentRepository taskCommentRepository = mock(TaskCommentRepository.class);
    private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
    private final TaskService taskService = new TaskService(
            taskRepository,
            taskCommentRepository,
            projectMemberRepository
    );

    @Test
    void 로그인_사용자가_속한_팀의_업무_상세와_댓글을_조회한다() {
        Task task = task(30L, 20L, 10L);
        ProjectMember author = projectMember(task.getProject(), user(7L));
        TaskComment comment = comment(40L, task, author, "완료했습니다.");

        when(taskRepository.findAccessibleDetailByIdAndUserId(30L, 7L))
                .thenReturn(Optional.of(task));
        when(taskCommentRepository.findAllWithAuthorByTaskId(30L))
                .thenReturn(List.of(comment));

        var response = taskService.getTaskDetail(7L, 30L);

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.projectId()).isEqualTo(20L);
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().get(0).mine()).isTrue();
        assertThat(response.comments().get(0).content()).isEqualTo("완료했습니다.");
    }

    @Test
    void 댓글_작성자는_요청값이_아닌_로그인_사용자로_지정된다() {
        Task task = task(30L, 20L, 10L);
        ProjectMember currentMember = projectMember(task.getProject(), user(7L));
        when(taskRepository.findAccessibleDetailByIdAndUserId(30L, 7L))
                .thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProject_IdAndUser_Id(20L, 7L))
                .thenReturn(Optional.of(currentMember));
        when(taskCommentRepository.save(any(TaskComment.class))).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0);
            comment.setId(40L);
            comment.setCreatedAt(OffsetDateTime.parse("2026-11-22T12:00:00+09:00"));
            return comment;
        });

        var response = taskService.createComment(
                7L,
                30L,
                new CreateTaskCommentRequest("  새 댓글  ")
        );

        ArgumentCaptor<TaskComment> captor = ArgumentCaptor.forClass(TaskComment.class);
        verify(taskCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(currentMember);
        assertThat(captor.getValue().getContent()).isEqualTo("새 댓글");
        assertThat(response.authorUserId()).isEqualTo(7L);
        assertThat(response.mine()).isTrue();
    }

    @Test
    void 팀에_속하지_않은_사용자에게는_업무_존재도_노출하지_않는다() {
        when(taskRepository.findAccessibleDetailByIdAndUserId(30L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskDetail(7L, 30L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verifyNoInteractions(taskCommentRepository, projectMemberRepository);
    }

    @Test
    void 업무_삭제_시_댓글을_먼저_삭제한다() {
        Task task = task(30L, 20L, 10L);
        when(taskRepository.findAccessibleDetailByIdAndUserId(30L, 7L))
                .thenReturn(Optional.of(task));

        taskService.deleteTask(7L, 30L);

        verify(taskCommentRepository).deleteAllByTaskId(30L);
        verify(taskRepository).delete(task);
    }

    private Task task(Long taskId, Long projectId, Long teamId) {
        Team team = new Team();
        team.setId(teamId);
        Project project = new Project();
        project.setId(projectId);
        project.setName("프로젝트");
        project.setTeam(team);
        Task task = new Task();
        task.setId(taskId);
        task.setProject(project);
        task.setTitle("업무");
        return task;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setNickname("사용자-" + id);
        return user;
    }

    private ProjectMember projectMember(Project project, User user) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        return member;
    }

    private TaskComment comment(
            Long id,
            Task task,
            ProjectMember author,
            String content
    ) {
        TaskComment comment = new TaskComment();
        comment.setId(id);
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setCreatedAt(OffsetDateTime.parse("2026-11-22T12:00:00+09:00"));
        return comment;
    }
}
