package com.flowablelab.infrastructure.flowable.delegate;

import com.flowablelab.application.usecase.GrantAccessUseCase;
import com.flowablelab.domain.request.AccessLevel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Date;

/** Service Task "Conceder acesso", executada após a Tarefa TI. */
@Component("grantAccessDelegate")
public class GrantAccessDelegate implements JavaDelegate {

    private final GrantAccessUseCase grantAccess;
    private final Clock clock;

    public GrantAccessDelegate(GrantAccessUseCase grantAccess, Clock clock) {
        this.grantAccess = grantAccess;
        this.clock = clock;
    }

    @Override
    public void execute(DelegateExecution execution) {
        grantAccess.grant(
                ProcessVariables.requiredString(execution, "requestNumber"),
                ProcessVariables.requiredString(execution, "requester"),
                ProcessVariables.requiredString(execution, "system"),
                AccessLevel.valueOf(ProcessVariables.requiredString(execution, "accessLevel")),
                ProcessVariables.string(execution, "itProvisioningCompletedBy"));
        execution.setVariable("accessGrantedAt", Date.from(clock.instant()));
    }
}
