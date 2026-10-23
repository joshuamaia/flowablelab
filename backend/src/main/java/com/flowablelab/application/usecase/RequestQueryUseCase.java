package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Filters.RequestFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.model.Views.RequestView;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.request.WorkflowRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RequestQueryUseCase {

    private final WorkflowRequestRepository requests;
    private final ProcessEnginePort engine;

    public RequestQueryUseCase(WorkflowRequestRepository requests, ProcessEnginePort engine) {
        this.requests = requests;
        this.engine = engine;
    }

    public PageResult<RequestView> search(RequestFilter filter, PageQuery page) {
        PageResult<WorkflowRequest> result = requests.search(filter, page);
        // Uma única consulta ao motor para todas as instâncias da página.
        Map<String, List<String>> steps = engine.currentStepNames(result.content().stream()
                .map(WorkflowRequest::processInstanceId)
                .filter(Objects::nonNull)
                .toList());
        return result.map(request -> new RequestView(request,
                steps.getOrDefault(request.processInstanceId(), List.of())));
    }

    public RequestView get(UUID id) {
        WorkflowRequest request = requests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação", id.toString()));
        List<String> steps = request.processInstanceId() == null ? List.of()
                : engine.currentStepNames(List.of(request.processInstanceId()))
                        .getOrDefault(request.processInstanceId(), List.of());
        return new RequestView(request, steps);
    }
}
