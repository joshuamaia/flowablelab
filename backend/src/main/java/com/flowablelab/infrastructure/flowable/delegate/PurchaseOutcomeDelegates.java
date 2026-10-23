package com.flowablelab.infrastructure.flowable.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Service Tasks finais do processo de compra ("Compra aprovada" e "Compra rejeitada"). */
@Configuration
public class PurchaseOutcomeDelegates {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOutcomeDelegates.class);

    /** Gera o número do pedido de compra a partir do número da solicitação. */
    @Bean
    public JavaDelegate purchaseOrderDelegate() {
        return (DelegateExecution execution) -> {
            String number = ProcessVariables.requiredString(execution, "requestNumber");
            String purchaseOrder = number.replace("REQ-", "PO-");
            execution.setVariable("purchaseOrderNumber", purchaseOrder);
            log.info("[{}] Compra aprovada: pedido {} emitido", execution.getProcessInstanceId(), purchaseOrder);
        };
    }

    /** Registra o motivo da rejeição informado pelo gestor. */
    @Bean
    public JavaDelegate purchaseRejectionDelegate() {
        return (DelegateExecution execution) -> {
            String reason = ProcessVariables.string(execution, "managerReviewComment");
            execution.setVariable("rejectionReason", reason);
            log.info("[{}] Compra rejeitada. Motivo: {}", execution.getProcessInstanceId(), reason);
        };
    }
}
