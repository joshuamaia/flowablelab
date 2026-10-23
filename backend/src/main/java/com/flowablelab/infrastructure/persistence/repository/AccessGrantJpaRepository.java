package com.flowablelab.infrastructure.persistence.repository;

import com.flowablelab.infrastructure.persistence.entity.AccessGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessGrantJpaRepository extends JpaRepository<AccessGrantEntity, Long> {

    boolean existsByRequestNumber(String requestNumber);
}
