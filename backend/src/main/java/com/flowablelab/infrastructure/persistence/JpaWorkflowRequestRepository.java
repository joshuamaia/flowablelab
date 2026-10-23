package com.flowablelab.infrastructure.persistence;

import com.flowablelab.application.model.Filters.RequestFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.WorkflowRequest;
import com.flowablelab.infrastructure.persistence.entity.WorkflowRequestEntity;
import com.flowablelab.infrastructure.persistence.repository.WorkflowRequestJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adaptador JPA da porta {@link WorkflowRequestRepository}. */
@Component
public class JpaWorkflowRequestRepository implements WorkflowRequestRepository {

    private static final Set<String> SORTABLE = Set.of("openedAt", "number", "status", "type", "requester");

    private final WorkflowRequestJpaRepository jpa;
    private final Clock clock;

    public JpaWorkflowRequestRepository(WorkflowRequestJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public WorkflowRequest save(WorkflowRequest request) {
        WorkflowRequestEntity entity = jpa.findById(request.id()).orElseGet(() -> new WorkflowRequestEntity(
                request.id(), request.number(), request.type(), request.requester(), request.summary(),
                request.amount(), request.openedAt(), request.demo()));
        entity.updateState(request.status(), request.statusDetail(), request.processInstanceId(),
                request.processDefinitionId(), request.finishedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<WorkflowRequest> findById(UUID id) {
        return jpa.findById(id).map(JpaWorkflowRequestRepository::toDomain);
    }

    @Override
    public Optional<WorkflowRequest> findByNumber(String number) {
        return jpa.findByNumber(number).map(JpaWorkflowRequestRepository::toDomain);
    }

    @Override
    public Optional<WorkflowRequest> findByProcessInstanceId(String processInstanceId) {
        return jpa.findByProcessInstanceId(processInstanceId).map(JpaWorkflowRequestRepository::toDomain);
    }

    @Override
    public PageResult<WorkflowRequest> search(RequestFilter filter, PageQuery page) {
        Specification<WorkflowRequestEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.requester() != null && !filter.requester().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("requester")),
                        "%" + filter.requester().trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        String sortBy = page.sortBy() != null && SORTABLE.contains(page.sortBy()) ? page.sortBy() : "openedAt";
        Sort sort = Sort.by(page.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy)
                .and(Sort.by(Sort.Direction.DESC, "number"));
        Page<WorkflowRequestEntity> result = jpa.findAll(specification, PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(result.getContent().stream().map(JpaWorkflowRequestRepository::toDomain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    public String nextRequestNumber() {
        return "REQ-%d-%06d".formatted(Year.now(clock).getValue(), jpa.nextNumber());
    }

    @Override
    public Map<RequestStatus, Long> countByStatus() {
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (Object[] row : jpa.countGroupedByStatus()) {
            counts.put((RequestStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public boolean existsDemoData() {
        return jpa.existsByDemoTrue();
    }

    private static WorkflowRequest toDomain(WorkflowRequestEntity e) {
        return new WorkflowRequest(e.getId(), e.getNumber(), e.getType(), e.getRequester(), e.getSummary(),
                e.getAmount(), e.getOpenedAt(), e.isDemo(), e.getStatus(), e.getProcessInstanceId(),
                e.getProcessDefinitionId(), e.getFinishedAt(), e.getStatusDetail());
    }
}
