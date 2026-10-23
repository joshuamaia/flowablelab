package com.flowablelab.application.port.out;

import com.flowablelab.application.model.Filters.RequestFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.WorkflowRequest;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRequestRepository {

    WorkflowRequest save(WorkflowRequest request);

    Optional<WorkflowRequest> findById(UUID id);

    Optional<WorkflowRequest> findByNumber(String number);

    Optional<WorkflowRequest> findByProcessInstanceId(String processInstanceId);

    PageResult<WorkflowRequest> search(RequestFilter filter, PageQuery page);

    /** Gera o próximo número sequencial (ex.: REQ-2026-000042), usado também como businessKey. */
    String nextRequestNumber();

    Map<RequestStatus, Long> countByStatus();

    boolean existsDemoData();
}
