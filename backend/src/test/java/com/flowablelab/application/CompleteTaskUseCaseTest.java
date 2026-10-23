package com.flowablelab.application;

import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.usecase.CompleteTaskUseCase;
import com.flowablelab.domain.exception.InvalidTaskActionException;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.exception.TaskAlreadyCompletedException;
import com.flowablelab.domain.exception.UserNotAllowedException;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.TaskStatus;
import com.flowablelab.domain.workflow.WorkTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteTaskUseCaseTest {

    @Mock
    TaskPort tasks;
    @Mock
    ProcessEnginePort engine;
    @InjectMocks
    CompleteTaskUseCase useCase;

    private static WorkTask managerReview() {
        return new WorkTask("t1", "Análise do gestor", "managerReview", "pi-1", "purchaseApproval:1:x",
                "purchaseApproval", "Aprovação de compra", null, List.of("gestor"), Instant.now(), null, null,
                TaskStatus.PENDING, null, null, Map.of("requestNumber", "REQ-1"));
    }

    @Test
    void tarefaInexistente() {
        when(tasks.findActive("x")).thenReturn(Optional.empty());
        when(tasks.findHistoric("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.complete("x", DemoUser.GESTOR, TaskAction.APPROVE, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tarefaJaConcluida() {
        when(tasks.findActive("t1")).thenReturn(Optional.empty());
        when(tasks.findHistoric("t1")).thenReturn(Optional.of(managerReview()));
        assertThatThrownBy(() -> useCase.complete("t1", DemoUser.GESTOR, TaskAction.APPROVE, null))
                .isInstanceOf(TaskAlreadyCompletedException.class);
    }

    @Test
    void usuarioForaDoGrupoNaoPodeConcluir() {
        when(tasks.findActive("t1")).thenReturn(Optional.of(managerReview()));
        assertThatThrownBy(() -> useCase.complete("t1", DemoUser.TI, TaskAction.APPROVE, null))
                .isInstanceOf(UserNotAllowedException.class);
        verify(tasks, never()).complete(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void acaoInvalidaNaoChegaAoMotor() {
        when(tasks.findActive("t1")).thenReturn(Optional.of(managerReview()));
        assertThatThrownBy(() -> useCase.complete("t1", DemoUser.GESTOR, TaskAction.REJECT, ""))
                .isInstanceOf(InvalidTaskActionException.class);
        verify(tasks, never()).complete(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void aprovacaoValidaConcluiComVariaveisDeDecisao() {
        when(tasks.findActive("t1")).thenReturn(Optional.of(managerReview()));
        when(engine.findInstance("pi-1")).thenReturn(Optional.of(new ProcessInstanceInfo("pi-1", "REQ-1", null,
                null, "purchaseApproval", null, 1, null, Instant.now(), Instant.now(), 10L, "endApproved", null)));
        when(tasks.activeTasksOf("pi-1")).thenReturn(List.of());

        var result = useCase.complete("t1", DemoUser.GESTOR, TaskAction.APPROVE, "ok");

        verify(tasks).complete(eq("t1"), eq("gestor"), eq(TaskAction.APPROVE), eq("ok"),
                eq(Map.of("approved", true, "managerReviewComment", "ok", "managerReviewCompletedBy", "gestor")));
        assertThat(result.processEnded()).isTrue();
        assertThat(result.nextTasks()).isEmpty();
    }
}
