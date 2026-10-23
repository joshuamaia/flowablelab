package com.flowablelab.domain.request;

import com.flowablelab.domain.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Solicitação de negócio. É correlacionada à instância BPMN pelo businessKey
 * (= número da solicitação) e pelo processInstanceId.
 */
public class WorkflowRequest {

    private final UUID id;
    private final String number;
    private final RequestType type;
    private final String requester;
    private final String summary;
    private final BigDecimal amount;
    private final Instant openedAt;
    private final boolean demo;
    private RequestStatus status;
    private String processInstanceId;
    private String processDefinitionId;
    private Instant finishedAt;
    private String statusDetail;

    public WorkflowRequest(UUID id, String number, RequestType type, String requester, String summary,
                           BigDecimal amount, Instant openedAt, boolean demo, RequestStatus status,
                           String processInstanceId, String processDefinitionId, Instant finishedAt,
                           String statusDetail) {
        this.id = Objects.requireNonNull(id);
        this.number = Objects.requireNonNull(number);
        this.type = Objects.requireNonNull(type);
        this.requester = requester;
        this.summary = summary;
        this.amount = amount;
        this.openedAt = Objects.requireNonNull(openedAt);
        this.demo = demo;
        this.status = Objects.requireNonNull(status);
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.finishedAt = finishedAt;
        this.statusDetail = statusDetail;
    }

    public static WorkflowRequest open(String number, RequestData data, Instant now, boolean demo) {
        return new WorkflowRequest(UUID.randomUUID(), number, data.type(), data.requester(), data.summary(),
                data.amount(), now, demo, RequestStatus.IN_PROGRESS, null, null, null, null);
    }

    public void attachProcess(String processInstanceId, String processDefinitionId) {
        if (this.processInstanceId != null) {
            throw new BusinessRuleViolationException(
                    "A solicitação %s já possui um processo associado.".formatted(number));
        }
        this.processInstanceId = Objects.requireNonNull(processInstanceId);
        this.processDefinitionId = processDefinitionId;
    }

    /** Registra o desfecho informado pelo evento de fim do processo. */
    public void finish(RequestStatus outcome, Instant at) {
        if (!outcome.isFinal()) {
            throw new BusinessRuleViolationException("Desfecho inválido para encerramento: " + outcome);
        }
        if (status.isFinal()) {
            throw new BusinessRuleViolationException("A solicitação %s já foi encerrada.".formatted(number));
        }
        this.status = outcome;
        this.finishedAt = at;
        this.statusDetail = null;
    }

    /** Uma etapa automática falhou definitivamente (job movido para a fila de falhas). */
    public void markFailed(String detail) {
        if (!status.isFinal()) {
            this.status = RequestStatus.FAILED;
            this.statusDetail = detail;
        }
    }

    public UUID id() { return id; }
    public String number() { return number; }
    public RequestType type() { return type; }
    public String requester() { return requester; }
    public String summary() { return summary; }
    public BigDecimal amount() { return amount; }
    public Instant openedAt() { return openedAt; }
    public boolean demo() { return demo; }
    public RequestStatus status() { return status; }
    public String processInstanceId() { return processInstanceId; }
    public String processDefinitionId() { return processDefinitionId; }
    public Instant finishedAt() { return finishedAt; }
    public String statusDetail() { return statusDetail; }
}
