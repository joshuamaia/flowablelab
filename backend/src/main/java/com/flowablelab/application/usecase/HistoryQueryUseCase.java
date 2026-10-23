package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Views.ProcessInstanceDetails;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.WorkTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Histórico de execução (timeline) obtido do HistoryService do Flowable. */
@Service
@Transactional(readOnly = true)
public class HistoryQueryUseCase {

    private final ProcessEnginePort engine;
    private final TaskPort tasks;
    private final WorkflowRequestRepository requests;

    public HistoryQueryUseCase(ProcessEnginePort engine, TaskPort tasks, WorkflowRequestRepository requests) {
        this.engine = engine;
        this.tasks = tasks;
        this.requests = requests;
    }

    public ProcessInstanceDetails process(String id) {
        ProcessInstanceInfo instance = instance(id);
        return new ProcessInstanceDetails(instance, requests.findByProcessInstanceId(id).orElse(null),
                tasks.activeTasksOf(id), engine.failures(id));
    }

    /** Atividades executadas (sem sequence flows), enriquecidas com decisões e comentários das tarefas. */
    public List<ActivityExecution> activities(String id) {
        instance(id);
        Map<String, WorkTask> tasksById = tasks.historicTasksOf(id).stream()
                .collect(Collectors.toMap(WorkTask::id, Function.identity()));
        return engine.activityExecutions(id).stream()
                .filter(execution -> !execution.isSequenceFlow())
                .map(execution -> {
                    WorkTask task = execution.taskId() == null ? null : tasksById.get(execution.taskId());
                    return task == null ? execution : execution.withDecision(task.decision(), task.comment());
                })
                .toList();
    }

    public List<WorkTask> tasks(String id) {
        instance(id);
        return tasks.historicTasksOf(id);
    }

    private ProcessInstanceInfo instance(String id) {
        return engine.findInstance(id).orElseThrow(() -> new ResourceNotFoundException("Instância de processo", id));
    }
}
