package com.flowablelab.infrastructure.flowable;

import com.flowablelab.application.model.Filters.TaskFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.workflow.TaskStatus;
import com.flowablelab.domain.workflow.WorkTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adaptador da porta {@link TaskPort} sobre TaskService e HistoryService do Flowable. */
@Component
public class FlowableTaskAdapter implements TaskPort {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final IdentityService identityService;
    private final FlowableMappers mappers;

    public FlowableTaskAdapter(TaskService taskService, HistoryService historyService,
                               IdentityService identityService, FlowableMappers mappers) {
        this.taskService = taskService;
        this.historyService = historyService;
        this.identityService = identityService;
        this.mappers = mappers;
    }

    @Override
    public Optional<WorkTask> findActive(String taskId) {
        return Optional.ofNullable(taskService.createTaskQuery()
                        .taskId(taskId)
                        .includeProcessVariables()
                        .includeIdentityLinks()
                        .singleResult())
                .map(task -> mappers.task(task, TaskStatus.PENDING));
    }

    @Override
    public Optional<WorkTask> findHistoric(String taskId) {
        return Optional.ofNullable(historyService.createHistoricTaskInstanceQuery()
                        .taskId(taskId)
                        .finished()
                        .includeTaskLocalVariables()
                        .includeProcessVariables()
                        .includeIdentityLinks()
                        .singleResult())
                .map(task -> mappers.task(task, TaskStatus.COMPLETED));
    }

    @Override
    public PageResult<WorkTask> search(TaskFilter filter, PageQuery page) {
        if (filter.status() == TaskStatus.PENDING) {
            TaskQuery query = taskService.createTaskQuery().active();
            if (filter.user() != null) {
                query.or()
                        .taskAssignee(filter.user().id())
                        .taskCandidateGroup(filter.user().group())
                        .endOr();
            }
            if (filter.processDefinitionKey() != null && !filter.processDefinitionKey().isBlank()) {
                query.processDefinitionKey(filter.processDefinitionKey());
            }
            long total = query.count();
            query.includeProcessVariables().includeIdentityLinks().orderByTaskCreateTime();
            if (page.ascending()) {
                query.asc();
            } else {
                query.desc();
            }
            List<WorkTask> content = query.listPage(page.offset(), page.size()).stream()
                    .map(task -> mappers.task(task, TaskStatus.PENDING))
                    .toList();
            return new PageResult<>(content, page.page(), page.size(), total);
        }

        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().finished();
        if (filter.user() != null) {
            query.taskAssignee(filter.user().id());
        }
        if (filter.processDefinitionKey() != null && !filter.processDefinitionKey().isBlank()) {
            query.processDefinitionKey(filter.processDefinitionKey());
        }
        long total = query.count();
        query.includeTaskLocalVariables().includeProcessVariables().includeIdentityLinks()
                .orderByHistoricTaskInstanceEndTime();
        if (page.ascending()) {
            query.asc();
        } else {
            query.desc();
        }
        List<WorkTask> content = query.listPage(page.offset(), page.size()).stream()
                .map(task -> mappers.task(task, TaskStatus.COMPLETED))
                .toList();
        return new PageResult<>(content, page.page(), page.size(), total);
    }

    @Override
    public List<WorkTask> activeTasksOf(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .includeIdentityLinks()
                .orderByTaskCreateTime().asc()
                .list().stream()
                .map(task -> mappers.task(task, TaskStatus.PENDING))
                .toList();
    }

    @Override
    public List<WorkTask> historicTasksOf(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .orderByHistoricTaskInstanceStartTime().asc()
                .list().stream()
                .map(task -> mappers.task(task, task.getEndTime() == null ? TaskStatus.PENDING : TaskStatus.COMPLETED))
                .toList();
    }

    @Override
    public void complete(String taskId, String userId, TaskAction action, String comment,
                         Map<String, Object> variables) {
        identityService.setAuthenticatedUserId(userId);
        try {
            // O responsável fica registrado no histórico da tarefa e da atividade.
            taskService.setAssignee(taskId, userId);
            Map<String, Object> local = new HashMap<>();
            local.put("decision", action.name());
            local.put("decidedBy", userId);
            local.put("comment", comment == null ? "" : comment.trim());
            taskService.setVariablesLocal(taskId, local);
            taskService.complete(taskId, variables);
        } catch (RuntimeException e) {
            throw FlowableExceptions.translate(e);
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    @Override
    public long countPending() {
        return taskService.createTaskQuery().active().count();
    }
}
