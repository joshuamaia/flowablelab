package com.flowablelab.infrastructure.persistence.entity;

import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.RequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_request")
public class WorkflowRequestEntity {

    @Id
    private UUID id;

    @Column(name = "request_number", nullable = false, unique = true, updatable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, updatable = false)
    private RequestType type;

    @Column(nullable = false, updatable = false)
    private String requester;

    @Column(nullable = false, updatable = false)
    private String summary;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "status_detail")
    private String statusDetail;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "process_definition_id")
    private String processDefinitionId;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(nullable = false, updatable = false)
    private boolean demo;

    @Version
    private long version;

    protected WorkflowRequestEntity() {
    }

    public WorkflowRequestEntity(UUID id, String number, RequestType type, String requester, String summary,
                                 BigDecimal amount, Instant openedAt, boolean demo) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.requester = requester;
        this.summary = summary;
        this.amount = amount;
        this.openedAt = openedAt;
        this.demo = demo;
    }

    public void updateState(RequestStatus status, String statusDetail, String processInstanceId,
                            String processDefinitionId, Instant finishedAt) {
        this.status = status;
        this.statusDetail = statusDetail;
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.finishedAt = finishedAt;
    }

    public UUID getId() { return id; }
    public String getNumber() { return number; }
    public RequestType getType() { return type; }
    public String getRequester() { return requester; }
    public String getSummary() { return summary; }
    public BigDecimal getAmount() { return amount; }
    public RequestStatus getStatus() { return status; }
    public String getStatusDetail() { return statusDetail; }
    public String getProcessInstanceId() { return processInstanceId; }
    public String getProcessDefinitionId() { return processDefinitionId; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public boolean isDemo() { return demo; }
}
