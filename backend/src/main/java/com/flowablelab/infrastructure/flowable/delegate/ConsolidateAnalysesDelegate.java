package com.flowablelab.infrastructure.flowable.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service Task "Consolidar análises": só é alcançada depois que o Parallel Gateway de
 * junção recebe os dois ramos. A solicitação é aprovada apenas se ambas as análises aprovarem.
 */
@Component("consolidateAnalysesDelegate")
public class ConsolidateAnalysesDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ConsolidateAnalysesDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        boolean technical = ProcessVariables.bool(execution, "technicalApproved");
        boolean financial = ProcessVariables.bool(execution, "financialApproved");
        boolean approved = technical && financial;
        String summary = "Técnica: %s · Financeira: %s".formatted(
                technical ? "aprovada" : "rejeitada", financial ? "aprovada" : "rejeitada");
        execution.setVariable("consolidatedApproved", approved);
        execution.setVariable("consolidationSummary", summary);
        log.info("[{}] Análises consolidadas: {} => {}", execution.getProcessInstanceId(), summary,
                approved ? "APROVADA" : "REJEITADA");
    }
}
