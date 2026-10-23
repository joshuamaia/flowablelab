package com.flowablelab.domain.workflow;

import java.time.Instant;

/**
 * Uma execução de um elemento BPMN registrada no histórico do motor (atividades,
 * gateways, eventos e sequence flows). Um mesmo elemento pode ter várias execuções
 * (loops) ou execuções simultâneas (ramos paralelos).
 */
public record ActivityExecution(
        String id,
        String activityId,
        String activityName,
        String activityType,
        String executionId,
        String taskId,
        String assignee,
        Instant startTime,
        Instant endTime,
        Long durationMillis,
        String decision,
        String comment) {

    public boolean isActive() {
        return endTime == null;
    }

    public boolean isSequenceFlow() {
        return "sequenceFlow".equals(activityType);
    }

    public ActivityExecution withDecision(String decision, String comment) {
        return new ActivityExecution(id, activityId, activityName, activityType, executionId, taskId, assignee,
                startTime, endTime, durationMillis, decision, comment);
    }
}
