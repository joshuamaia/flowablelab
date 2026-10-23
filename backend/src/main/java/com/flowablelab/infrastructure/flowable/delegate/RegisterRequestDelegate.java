package com.flowablelab.infrastructure.flowable.delegate;

import com.flowablelab.application.usecase.RequestLifecycleUseCase;
import com.flowablelab.domain.exception.BusinessRuleViolationException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Date;

/**
 * Service Task "Registrar solicitação": garante a correlação entre a instância BPMN
 * e a solicitação de negócio (businessKey) e carimba a data de registro.
 */
@Component("registerRequestDelegate")
public class RegisterRequestDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RegisterRequestDelegate.class);

    private final RequestLifecycleUseCase lifecycle;
    private final Clock clock;

    public RegisterRequestDelegate(RequestLifecycleUseCase lifecycle, Clock clock) {
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String number = ProcessVariables.requiredString(execution, "requestNumber");
        if (lifecycle.statusOf(number) == null) {
            throw new BusinessRuleViolationException(
                    "A instância %s não possui solicitação de negócio correspondente (%s)."
                            .formatted(execution.getProcessInstanceId(), number));
        }
        execution.setVariable("registeredAt", Date.from(clock.instant()));
        log.info("[{}] Solicitação {} registrada", execution.getProcessInstanceId(), number);
    }
}
