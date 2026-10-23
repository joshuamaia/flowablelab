package com.flowablelab.infrastructure.flowable;

import com.flowablelab.application.usecase.RequestLifecycleUseCase;
import com.flowablelab.domain.request.RequestStatus;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Execution listener dos End Events. Uso no BPMN:
 * {@code <flowable:executionListener event="start" expression="${requestOutcome.record(execution, 'APPROVED')}"/>}
 * Executa na mesma transação do motor, mantendo a solicitação de negócio consistente.
 */
@Component("requestOutcome")
public class RequestOutcomeListener {

    private final RequestLifecycleUseCase lifecycle;

    public RequestOutcomeListener(RequestLifecycleUseCase lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void record(DelegateExecution execution, String outcome) {
        Object number = execution.getVariable("requestNumber");
        lifecycle.recordOutcome(number == null ? null : number.toString(), execution.getProcessInstanceId(),
                RequestStatus.valueOf(outcome));
    }
}
