package com.flowablelab.interfaces.rest;

import com.flowablelab.application.model.Filters.TaskFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.Views.TaskCompletion;
import com.flowablelab.application.usecase.CompleteTaskUseCase;
import com.flowablelab.application.usecase.TaskQueryUseCase;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.TaskStatus;
import com.flowablelab.interfaces.rest.dto.CompleteTaskRequest;
import com.flowablelab.interfaces.rest.dto.Responses.PageResponse;
import com.flowablelab.interfaces.rest.dto.Responses.TaskCompletionResponse;
import com.flowablelab.interfaces.rest.dto.Responses.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tarefas", description = "User Tasks do Flowable")
public class TaskController {

    private final TaskQueryUseCase taskQuery;
    private final CompleteTaskUseCase completeTask;

    public TaskController(TaskQueryUseCase taskQuery, CompleteTaskUseCase completeTask) {
        this.taskQuery = taskQuery;
        this.completeTask = completeTask;
    }

    @GetMapping
    @Operation(summary = "Consulta tarefas por responsável, processo e status",
            description = "PENDING: tarefas elegíveis para o usuário (grupo candidato ou atribuídas). "
                    + "COMPLETED: tarefas concluídas pelo usuário.")
    public PageResponse<TaskResponse> list(
            @Parameter(description = "colaborador, gestor ou ti (vazio = todos)")
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String processKey,
            @RequestParam(defaultValue = "PENDING") TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String direction) {
        DemoUser user = assignee == null || assignee.isBlank() ? null : DemoUser.fromId(assignee);
        return PageResponse.of(taskQuery.search(new TaskFilter(user, processKey, status),
                new PageQuery(page, size, "createTime", "asc".equalsIgnoreCase(direction))), TaskResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma tarefa pendente ou concluída")
    public TaskResponse get(@PathVariable String id) {
        return TaskResponse.from(taskQuery.get(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Conclui a tarefa com a ação permitida (APPROVE, REJECT ou COMPLETE)",
            description = "Valida a elegibilidade do perfil demonstrativo e a ação aceita pelo tipo de tarefa.")
    public TaskCompletionResponse complete(
            @PathVariable String id,
            @Valid @RequestBody CompleteTaskRequest body,
            @Parameter(description = DemoUserHeader.DESCRIPTION)
            @RequestHeader(name = DemoUserHeader.NAME) String user) {
        TaskCompletion completion = completeTask.complete(id, DemoUser.fromId(user), body.action(), body.comment());
        return new TaskCompletionResponse(completion.taskId(), completion.processInstanceId(),
                completion.processEnded(), completion.nextTasks().stream().map(TaskResponse::from).toList());
    }
}
