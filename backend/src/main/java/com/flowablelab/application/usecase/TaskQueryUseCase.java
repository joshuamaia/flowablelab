package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Filters.TaskFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.model.Views.TaskView;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskQueryUseCase {

    private final TaskPort tasks;

    public TaskQueryUseCase(TaskPort tasks) {
        this.tasks = tasks;
    }

    public PageResult<TaskView> search(TaskFilter filter, PageQuery page) {
        return tasks.search(filter, page).map(TaskView::of);
    }

    public TaskView get(String taskId) {
        return tasks.findActive(taskId)
                .or(() -> tasks.findHistoric(taskId))
                .map(TaskView::of)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa", taskId));
    }
}
