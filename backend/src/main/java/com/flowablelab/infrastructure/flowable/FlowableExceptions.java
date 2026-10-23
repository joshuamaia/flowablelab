package com.flowablelab.infrastructure.flowable;

import com.flowablelab.domain.exception.DomainException;
import com.flowablelab.domain.exception.WorkflowExecutionException;
import org.flowable.common.engine.api.FlowableException;

/** Traduz exceções do motor para exceções de domínio, preservando regras de negócio violadas em delegates. */
final class FlowableExceptions {

    private FlowableExceptions() {
    }

    static RuntimeException translate(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof DomainException domain) {
                return domain;
            }
            current = current.getCause();
        }
        if (exception instanceof FlowableException) {
            return new WorkflowExecutionException(
                    "Falha ao executar uma etapa do processo no motor Flowable: " + exception.getMessage(), exception);
        }
        return exception;
    }
}
