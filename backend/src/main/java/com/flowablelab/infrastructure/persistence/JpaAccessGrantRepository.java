package com.flowablelab.infrastructure.persistence;

import com.flowablelab.application.port.out.AccessGrantRepository;
import com.flowablelab.domain.access.AccessGrant;
import com.flowablelab.infrastructure.persistence.entity.AccessGrantEntity;
import com.flowablelab.infrastructure.persistence.repository.AccessGrantJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaAccessGrantRepository implements AccessGrantRepository {

    private final AccessGrantJpaRepository jpa;

    public JpaAccessGrantRepository(AccessGrantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByRequestNumber(String requestNumber) {
        return jpa.existsByRequestNumber(requestNumber);
    }

    @Override
    public void save(AccessGrant grant) {
        jpa.save(new AccessGrantEntity(grant.employee(), grant.system(), grant.level(), grant.requestNumber(),
                grant.provisionedBy(), grant.grantedAt()));
    }
}
