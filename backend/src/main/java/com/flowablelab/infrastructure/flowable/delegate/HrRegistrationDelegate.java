package com.flowablelab.infrastructure.flowable.delegate;

import com.flowablelab.application.usecase.RegisterVacationUseCase;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Date;

/**
 * Service Task assíncrona "Registro RH". Se o colaborador já tiver férias registradas no
 * período, a regra de negócio lança exceção: o job é reexecutado conforme o ciclo de
 * retentativas do BPMN e, esgotadas as tentativas, vai para a fila de dead letter —
 * o visualizador então destaca a atividade em vermelho.
 */
@Component("hrRegistrationDelegate")
public class HrRegistrationDelegate implements JavaDelegate {

    private final RegisterVacationUseCase registerVacation;
    private final Clock clock;

    public HrRegistrationDelegate(RegisterVacationUseCase registerVacation, Clock clock) {
        this.registerVacation = registerVacation;
        this.clock = clock;
    }

    @Override
    public void execute(DelegateExecution execution) {
        registerVacation.register(
                ProcessVariables.requiredString(execution, "requestNumber"),
                ProcessVariables.requiredString(execution, "requester"),
                ProcessVariables.date(execution, "startDate"),
                ProcessVariables.date(execution, "endDate"));
        execution.setVariable("hrRegisteredAt", Date.from(clock.instant()));
    }
}
