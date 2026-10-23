package com.flowablelab.application.usecase;

import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.WorkflowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Mantém a solicitação de negócio sincronizada com eventos do motor (fim do processo
 * e falhas definitivas de jobs). É chamado dentro da transação do próprio Flowable.
 */
@Service
public class RequestLifecycleUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestLifecycleUseCase.class);

    private final WorkflowRequestRepository requests;
    private final Clock clock;

    public RequestLifecycleUseCase(WorkflowRequestRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    @Transactional
    public void recordOutcome(String requestNumber, String processInstanceId, RequestStatus outcome) {
        try (var ignored = MDC.putCloseable("processInstanceId", processInstanceId)) {
            WorkflowRequest request = requests.findByNumber(requestNumber).orElse(null);
            if (request == null) {
                log.warn("Fim de processo sem solicitação de negócio associada (businessKey={})", requestNumber);
                return;
            }
            request.finish(outcome, clock.instant());
            requests.save(request);
            log.info("Solicitação {} encerrada com desfecho {}", requestNumber, outcome);
        }
    }

    @Transactional
    public void recordFailure(String processInstanceId, String elementId, String message) {
        try (var ignored = MDC.putCloseable("processInstanceId", processInstanceId)) {
            requests.findByProcessInstanceId(processInstanceId).ifPresent(request -> {
                request.markFailed("Falha na etapa '%s': %s".formatted(elementId, message));
                requests.save(request);
                log.error("Solicitação {} marcada como FAILED: etapa {} falhou definitivamente ({})",
                        request.number(), elementId, message);
            });
        }
    }

    public RequestStatus statusOf(String requestNumber) {
        return requests.findByNumber(requestNumber).map(WorkflowRequest::status).orElse(null);
    }
}
