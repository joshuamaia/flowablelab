package com.flowablelab.domain.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Uma User Task (pendente ou histórica) independente das classes do motor. */
public record WorkTask(
        String id,
        String name,
        String taskDefinitionKey,
        String processInstanceId,
        String processDefinitionId,
        String processDefinitionKey,
        String processDefinitionName,
        String assignee,
        List<String> candidateGroups,
        Instant createdAt,
        Instant endedAt,
        Long durationMillis,
        TaskStatus status,
        String decision,
        String comment,
        Map<String, Object> processVariables) {

    public WorkTask {
        candidateGroups = candidateGroups == null ? List.of() : List.copyOf(candidateGroups);
        processVariables = processVariables == null ? Map.of() : processVariables;
    }

    public String businessKey() {
        Object number = processVariables.get("requestNumber");
        return number == null ? null : number.toString();
    }
}
