package com.flowablelab.domain.exception;

/** Falha técnica ao executar uma etapa automática do workflow (ex.: Service Task). */
public class WorkflowExecutionException extends DomainException {

    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
