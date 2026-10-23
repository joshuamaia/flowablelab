package com.flowablelab.application.model;

import com.flowablelab.domain.request.WorkflowRequest;
import com.flowablelab.domain.task.TaskRule;
import com.flowablelab.domain.task.TaskRules;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ElementFailure;
import com.flowablelab.domain.workflow.ElementState;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.WorkTask;

import java.util.List;

/** Modelos de leitura devolvidos pelos casos de uso. */
public final class Views {

    private Views() {
    }

    public record RequestView(WorkflowRequest request, List<String> currentSteps) {
    }

    public record TaskView(WorkTask task, TaskRule rule) {

        public static TaskView of(WorkTask task) {
            return new TaskView(task, TaskRules.forTask(task.taskDefinitionKey()));
        }
    }

    public record ProcessInstanceDetails(ProcessInstanceInfo instance, WorkflowRequest request,
                                         List<WorkTask> activeTasks, List<ElementFailure> failures) {
    }

    /** Execuções reais + estado consolidado por elemento, consumidos pelo visualizador BPMN. */
    public record DiagramState(String processInstanceId, String processDefinitionId, boolean ended,
                               List<ActivityExecution> executions, List<ElementState> elements) {
    }

    public record StartedRequest(WorkflowRequest request, List<WorkTask> activeTasks) {
    }

    public record TaskCompletion(String taskId, String processInstanceId, boolean processEnded,
                                 List<WorkTask> nextTasks) {
    }

    public record DashboardSummary(long started, long running, long completed, long pendingTasks,
                                   long approved, long rejected, long failed) {
    }

    public record ProcessTypeStats(String processDefinitionKey, String name, long running, long completed) {
    }
}
