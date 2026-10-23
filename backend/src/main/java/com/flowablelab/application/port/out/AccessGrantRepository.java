package com.flowablelab.application.port.out;

import com.flowablelab.domain.access.AccessGrant;

public interface AccessGrantRepository {

    boolean existsByRequestNumber(String requestNumber);

    void save(AccessGrant grant);
}
