package com.flowablelab.application.usecase;

import com.flowablelab.application.port.out.AccessGrantRepository;
import com.flowablelab.domain.access.AccessGrant;
import com.flowablelab.domain.request.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class GrantAccessUseCase {

    private static final Logger log = LoggerFactory.getLogger(GrantAccessUseCase.class);

    private final AccessGrantRepository grants;
    private final Clock clock;

    public GrantAccessUseCase(AccessGrantRepository grants, Clock clock) {
        this.grants = grants;
        this.clock = clock;
    }

    /** Idempotente por número de solicitação. */
    @Transactional
    public void grant(String requestNumber, String employee, String system, AccessLevel level, String provisionedBy) {
        if (grants.existsByRequestNumber(requestNumber)) {
            return;
        }
        grants.save(new AccessGrant(employee, system, level, requestNumber, provisionedBy, clock.instant()));
        log.info("Acesso {} ao sistema {} concedido para {} (solicitação {})", level, system, employee, requestNumber);
    }
}
