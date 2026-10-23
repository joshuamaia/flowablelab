package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Views.DashboardSummary;
import com.flowablelab.application.model.Views.ProcessTypeStats;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.request.RequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardUseCase {

    private final ProcessEnginePort engine;
    private final TaskPort tasks;
    private final WorkflowRequestRepository requests;

    public DashboardUseCase(ProcessEnginePort engine, TaskPort tasks, WorkflowRequestRepository requests) {
        this.engine = engine;
        this.tasks = tasks;
        this.requests = requests;
    }

    public DashboardSummary summary() {
        Map<RequestStatus, Long> byStatus = statusDistribution();
        return new DashboardSummary(
                engine.countInstances(null),
                engine.countInstances(false),
                engine.countInstances(true),
                tasks.countPending(),
                byStatus.get(RequestStatus.APPROVED),
                byStatus.get(RequestStatus.REJECTED),
                byStatus.get(RequestStatus.FAILED));
    }

    /** Distribuição das solicitações de negócio por status (todos os status presentes, inclusive zero). */
    public Map<RequestStatus, Long> statusDistribution() {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            result.put(status, 0L);
        }
        result.putAll(requests.countByStatus());
        return result;
    }

    public List<ProcessTypeStats> processTypes() {
        return engine.statsByDefinition();
    }
}
