package com.flowablelab.domain.workflow;

import java.time.Instant;

public record ProcessVariable(String name, String type, Object value, Instant lastUpdated) {
}
