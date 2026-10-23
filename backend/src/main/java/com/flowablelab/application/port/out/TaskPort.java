package com.flowablelab.application.port.out;

import com.flowablelab.application.model.Filters.TaskFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.workflow.WorkTask;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TaskPort {

    Optional<WorkTask> findActive(String taskId);

    Optional<WorkTask> findHistoric(String taskId);

    PageResult<WorkTask> search(TaskFilter filter, PageQuery page);

    List<WorkTask> activeTasksOf(String processInstanceId);

    List<WorkTask> historicTasksOf(String processInstanceId);

    /**
     * Atribui a tarefa ao usuário, registra decisão e comentário como variáveis locais
     * (histórico) e conclui a tarefa com as variáveis de processo informadas.
     */
    void complete(String taskId, String userId, TaskAction action, String comment, Map<String, Object> variables);

    long countPending();
}
