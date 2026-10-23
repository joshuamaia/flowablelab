package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Views.TaskCompletion;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.exception.TaskAlreadyCompletedException;
import com.flowablelab.domain.exception.UserNotAllowedException;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.task.TaskRule;
import com.flowablelab.domain.task.TaskRules;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.WorkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CompleteTaskUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompleteTaskUseCase.class);

    private final TaskPort tasks;
    private final ProcessEnginePort engine;

    public CompleteTaskUseCase(TaskPort tasks, ProcessEnginePort engine) {
        this.tasks = tasks;
        this.engine = engine;
    }

    @Transactional
    public TaskCompletion complete(String taskId, DemoUser user, TaskAction action, String comment) {
        WorkTask task = tasks.findActive(taskId).orElseThrow(() -> tasks.findHistoric(taskId).isPresent()
                ? new TaskAlreadyCompletedException(taskId)
                : new ResourceNotFoundException("Tarefa", taskId));

        if (!user.canWorkOn(task.assignee(), task.candidateGroups())) {
            throw new UserNotAllowedException("O perfil '%s' não é responsável pela tarefa '%s' (grupos: %s)."
                    .formatted(user.id(), task.name(), task.candidateGroups()));
        }

        TaskRule rule = TaskRules.forTask(task.taskDefinitionKey());
        Map<String, Object> variables = new HashMap<>(rule.resolve(action, comment));
        variables.put(task.taskDefinitionKey() + "CompletedBy", user.id());

        try (var ignored = MDC.putCloseable("processInstanceId", task.processInstanceId())) {
            tasks.complete(taskId, user.id(), action, comment, variables);
            log.info("Tarefa '{}' ({}) concluída por {} com a ação {}", task.name(), taskId, user.id(), action);
        }

        boolean ended = engine.findInstance(task.processInstanceId()).map(ProcessInstanceInfo::ended).orElse(true);
        return new TaskCompletion(taskId, task.processInstanceId(), ended,
                tasks.activeTasksOf(task.processInstanceId()));
    }
}
