package com.flowablelab.interfaces.rest.dto;

import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.model.Views.DashboardSummary;
import com.flowablelab.application.model.Views.DiagramState;
import com.flowablelab.application.model.Views.ProcessInstanceDetails;
import com.flowablelab.application.model.Views.ProcessTypeStats;
import com.flowablelab.application.model.Views.RequestView;
import com.flowablelab.application.model.Views.TaskView;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.RequestType;
import com.flowablelab.domain.request.WorkflowRequest;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ActivityStatus;
import com.flowablelab.domain.workflow.ElementFailure;
import com.flowablelab.domain.workflow.ElementState;
import com.flowablelab.domain.workflow.ProcessDefinitionInfo;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.ProcessVariable;
import com.flowablelab.domain.workflow.TaskStatus;
import com.flowablelab.domain.workflow.WorkTask;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Contratos de resposta da API REST (nunca expõem entidades JPA ou objetos do motor). */
public final class Responses {

    private Responses() {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <S, T> PageResponse<T> of(PageResult<S> result, Function<S, T> mapper) {
            return new PageResponse<>(result.content().stream().map(mapper).toList(), result.page(), result.size(),
                    result.totalElements(), result.totalPages());
        }
    }

    public record UserResponse(String id, String displayName, String group) {

        public static UserResponse from(DemoUser user) {
            return new UserResponse(user.id(), user.displayName(), user.group());
        }
    }

    public record ProcessDefinitionResponse(String id, String key, String name, String description, int version,
                                            String deploymentId, String resourceName, boolean suspended) {

        public static ProcessDefinitionResponse from(ProcessDefinitionInfo d) {
            return new ProcessDefinitionResponse(d.id(), d.key(), d.name(), d.description(), d.version(),
                    d.deploymentId(), d.resourceName(), d.suspended());
        }
    }

    public record ProcessInstanceResponse(String id, String businessKey, String name, String processDefinitionId,
                                          String processDefinitionKey, String processDefinitionName,
                                          Integer processDefinitionVersion, String startUserId, Instant startTime,
                                          Instant endTime, Long durationMillis, String endActivityId,
                                          String status) {

        public static ProcessInstanceResponse from(ProcessInstanceInfo i) {
            String status = !i.ended() ? "RUNNING" : i.deleteReason() != null ? "CANCELED" : "COMPLETED";
            return new ProcessInstanceResponse(i.id(), i.businessKey(), i.name(), i.processDefinitionId(),
                    i.processDefinitionKey(), i.processDefinitionName(), i.processDefinitionVersion(),
                    i.startUserId(), i.startTime(), i.endTime(), i.durationMillis(), i.endActivityId(), status);
        }
    }

    public record RequestResponse(String id, String number, RequestType type, String typeLabel, String processKey,
                                  String requester, String summary, BigDecimal amount, RequestStatus status,
                                  String statusDetail, String processInstanceId, String processDefinitionId,
                                  Instant openedAt, Instant finishedAt, boolean demo, List<String> currentSteps) {

        public static RequestResponse from(WorkflowRequest r, List<String> currentSteps) {
            return new RequestResponse(r.id().toString(), r.number(), r.type(), r.type().label(),
                    r.type().processKey(), r.requester(), r.summary(), r.amount(), r.status(), r.statusDetail(),
                    r.processInstanceId(), r.processDefinitionId(), r.openedAt(), r.finishedAt(), r.demo(),
                    currentSteps);
        }

        public static RequestResponse from(RequestView view) {
            return from(view.request(), view.currentSteps());
        }
    }

    public record TaskResponse(String id, String name, String taskDefinitionKey, String processInstanceId,
                               String processDefinitionId, String processDefinitionKey,
                               String processDefinitionName, String businessKey, String assignee,
                               List<String> candidateGroups, Instant createdAt, Instant endedAt,
                               Long durationMillis, TaskStatus status, String decision, String comment,
                               List<TaskAction> allowedActions, boolean decisionTask,
                               Map<String, Object> variables) {

        public static TaskResponse from(TaskView view) {
            WorkTask t = view.task();
            return new TaskResponse(t.id(), t.name(), t.taskDefinitionKey(), t.processInstanceId(),
                    t.processDefinitionId(), t.processDefinitionKey(), t.processDefinitionName(), t.businessKey(),
                    t.assignee(), t.candidateGroups(), t.createdAt(), t.endedAt(), t.durationMillis(), t.status(),
                    t.decision(), t.comment(),
                    t.status() == TaskStatus.PENDING ? view.rule().allowedActions() : List.of(),
                    view.rule().isDecision(), t.processVariables());
        }

        public static TaskResponse from(WorkTask task) {
            return from(TaskView.of(task));
        }
    }

    public record StartProcessResponse(RequestResponse request, List<TaskResponse> activeTasks) {
    }

    public record TaskCompletionResponse(String taskId, String processInstanceId, boolean processEnded,
                                         List<TaskResponse> nextTasks) {
    }

    public record ActivityResponse(String id, String activityId, String name, String type, String executionId,
                                   String taskId, String assignee, Instant startTime, Instant endTime,
                                   Long durationMillis, ActivityStatus status, String decision, String comment) {

        public static ActivityResponse from(ActivityExecution a) {
            return new ActivityResponse(a.id(), a.activityId(), a.activityName(), a.activityType(), a.executionId(),
                    a.taskId(), a.assignee(), a.startTime(), a.endTime(), a.durationMillis(),
                    a.isActive() ? ActivityStatus.ACTIVE : ActivityStatus.COMPLETED, a.decision(), a.comment());
        }
    }

    public record ElementStateResponse(String elementId, ActivityStatus status, int executionCount,
                                       String failureMessage) {

        public static ElementStateResponse from(ElementState s) {
            return new ElementStateResponse(s.elementId(), s.status(), s.executionCount(), s.failureMessage());
        }
    }

    public record DiagramStateResponse(String processInstanceId, String processDefinitionId, boolean ended,
                                       List<ActivityResponse> executions, List<ElementStateResponse> elements) {

        public static DiagramStateResponse from(DiagramState s) {
            return new DiagramStateResponse(s.processInstanceId(), s.processDefinitionId(), s.ended(),
                    s.executions().stream().map(ActivityResponse::from).toList(),
                    s.elements().stream().map(ElementStateResponse::from).toList());
        }
    }

    public record FailureResponse(String elementId, String message) {

        public static FailureResponse from(ElementFailure f) {
            return new FailureResponse(f.elementId(), f.message());
        }
    }

    public record ProcessInstanceDetailsResponse(ProcessInstanceResponse instance, RequestResponse request,
                                                 List<TaskResponse> activeTasks, List<FailureResponse> failures) {

        public static ProcessInstanceDetailsResponse from(ProcessInstanceDetails d) {
            List<String> steps = d.activeTasks().stream().map(WorkTask::name).toList();
            return new ProcessInstanceDetailsResponse(ProcessInstanceResponse.from(d.instance()),
                    d.request() == null ? null : RequestResponse.from(d.request(), steps),
                    d.activeTasks().stream().map(TaskResponse::from).toList(),
                    d.failures().stream().map(FailureResponse::from).toList());
        }
    }

    public record VariableResponse(String name, String type, Object value, Instant lastUpdated) {

        public static VariableResponse from(ProcessVariable v) {
            return new VariableResponse(v.name(), v.type(), v.value(), v.lastUpdated());
        }
    }

    public record DashboardSummaryResponse(long started, long running, long completed, long pendingTasks,
                                           long approved, long rejected, long failed) {

        public static DashboardSummaryResponse from(DashboardSummary s) {
            return new DashboardSummaryResponse(s.started(), s.running(), s.completed(), s.pendingTasks(),
                    s.approved(), s.rejected(), s.failed());
        }
    }

    public record StatusCountResponse(RequestStatus status, long count) {
    }

    public record ProcessTypeResponse(String processDefinitionKey, String name, long running, long completed,
                                      long total) {

        public static ProcessTypeResponse from(ProcessTypeStats s) {
            return new ProcessTypeResponse(s.processDefinitionKey(), s.name(), s.running(), s.completed(),
                    s.running() + s.completed());
        }
    }
}
