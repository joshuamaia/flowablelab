package com.flowablelab.domain.workflow;

public record ProcessDefinitionInfo(
        String id,
        String key,
        String name,
        String description,
        int version,
        String deploymentId,
        String resourceName,
        boolean suspended) {
}
