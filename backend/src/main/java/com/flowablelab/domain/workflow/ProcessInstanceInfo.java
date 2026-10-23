package com.flowablelab.domain.workflow;

import java.time.Instant;

public record ProcessInstanceInfo(
        String id,
        String businessKey,
        String name,
        String processDefinitionId,
        String processDefinitionKey,
        String processDefinitionName,
        Integer processDefinitionVersion,
        String startUserId,
        Instant startTime,
        Instant endTime,
        Long durationMillis,
        String endActivityId,
        String deleteReason) {

    public boolean ended() {
        return endTime != null;
    }
}
