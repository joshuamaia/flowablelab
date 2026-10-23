package com.flowablelab.infrastructure.flowable.delegate;

import com.flowablelab.domain.request.PurchaseData;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service Task "Validação automática" da compra: reaplica as regras do domínio sobre as
 * variáveis do processo e classifica a prioridade. Dados inválidos interrompem o processo.
 */
@Component("purchaseValidationDelegate")
public class PurchaseValidationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PurchaseValidationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        PurchaseData purchase = new PurchaseData(
                ProcessVariables.string(execution, "requester"),
                ProcessVariables.string(execution, "description"),
                ProcessVariables.decimal(execution, "amount"),
                ProcessVariables.string(execution, "justification"));
        execution.setVariable("validated", true);
        execution.setVariable("priority", purchase.priority());
        log.info("[{}] Compra validada automaticamente: valor={} prioridade={}", execution.getProcessInstanceId(),
                purchase.amount(), purchase.priority());
    }
}
