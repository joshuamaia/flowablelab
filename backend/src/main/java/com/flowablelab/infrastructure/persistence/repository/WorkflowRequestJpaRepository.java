package com.flowablelab.infrastructure.persistence.repository;

import com.flowablelab.infrastructure.persistence.entity.WorkflowRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRequestJpaRepository
        extends JpaRepository<WorkflowRequestEntity, UUID>, JpaSpecificationExecutor<WorkflowRequestEntity> {

    Optional<WorkflowRequestEntity> findByNumber(String number);

    Optional<WorkflowRequestEntity> findByProcessInstanceId(String processInstanceId);

    boolean existsByDemoTrue();

    @Query("select r.status, count(r) from WorkflowRequestEntity r group by r.status")
    List<Object[]> countGroupedByStatus();

    @Query(value = "select nextval('business.workflow_request_number_seq')", nativeQuery = true)
    long nextNumber();
}
