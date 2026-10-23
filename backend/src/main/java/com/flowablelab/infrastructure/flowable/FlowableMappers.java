package com.flowablelab.infrastructure.flowable;

import com.flowablelab.domain.workflow.ProcessDefinitionInfo;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.TaskStatus;
import com.flowablelab.domain.workflow.WorkTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Conversões dos objetos internos do Flowable para os modelos independentes do motor. */
@Component
public class FlowableMappers {

    private final RepositoryService repositoryService;

    public FlowableMappers(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public static Instant instant(Date date) {
        return date == null ? null : date.toInstant();
    }

    public static ProcessDefinitionInfo definition(ProcessDefinition definition) {
        return new ProcessDefinitionInfo(definition.getId(), definition.getKey(), definition.getName(),
                definition.getDescription(), definition.getVersion(), definition.getDeploymentId(),
                definition.getResourceName(), definition.isSuspended());
    }

    public static ProcessInstanceInfo instance(HistoricProcessInstance instance) {
        return new ProcessInstanceInfo(instance.getId(), instance.getBusinessKey(), instance.getName(),
                instance.getProcessDefinitionId(), instance.getProcessDefinitionKey(),
                instance.getProcessDefinitionName(), instance.getProcessDefinitionVersion(),
                instance.getStartUserId(), instant(instance.getStartTime()), instant(instance.getEndTime()),
                instance.getDurationInMillis(), instance.getEndActivityId(), instance.getDeleteReason());
    }

    /** Converte para tipos simples e serializáveis em JSON. */
    public static Object safeValue(Object value) {
        return switch (value) {
            case null -> null;
            case String s -> s;
            case Number n -> n;
            case Boolean b -> b;
            case Date d -> d.toInstant().toString();
            case LocalDate d -> d.toString();
            case LocalDateTime d -> d.toString();
            case Instant i -> i.toString();
            default -> value.toString();
        };
    }

    public static Map<String, Object> safeVariables(Map<String, Object> variables) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (variables != null) {
            variables.forEach((name, value) -> result.put(name, safeValue(value)));
        }
        return result;
    }

    public WorkTask task(TaskInfo task, TaskStatus status) {
        ProcessDefinition definition = task.getProcessDefinitionId() == null ? null
                : repositoryService.getProcessDefinition(task.getProcessDefinitionId()); // cache do motor
        List<String> groups = task.getIdentityLinks() == null ? List.of() : task.getIdentityLinks().stream()
                .filter(link -> IdentityLinkType.CANDIDATE.equals(link.getType()))
                .map(IdentityLinkInfo::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, Object> local = task.getTaskLocalVariables();
        Instant endedAt = null;
        Long duration = null;
        if (task instanceof HistoricTaskInstance historic) {
            endedAt = instant(historic.getEndTime());
            duration = historic.getDurationInMillis();
            if (historic.getEndTime() != null && historic.getDeleteReason() != null) {
                status = TaskStatus.CANCELED;
            }
        }
        return new WorkTask(task.getId(), task.getName(), task.getTaskDefinitionKey(), task.getProcessInstanceId(),
                task.getProcessDefinitionId(),
                definition == null ? null : definition.getKey(),
                definition == null ? null : definition.getName(),
                task.getAssignee(), groups, instant(task.getCreateTime()), endedAt, duration, status,
                local == null ? null : (String) local.get("decision"),
                local == null ? null : (String) local.get("comment"),
                safeVariables(task.getProcessVariables()));
    }
}
