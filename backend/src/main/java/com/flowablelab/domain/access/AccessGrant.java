package com.flowablelab.domain.access;

import com.flowablelab.domain.request.AccessLevel;

import java.time.Instant;

/** Concessão de acesso registrada pela Service Task "Conceder acesso". */
public record AccessGrant(String employee, String system, AccessLevel level, String requestNumber,
                          String provisionedBy, Instant grantedAt) {
}
