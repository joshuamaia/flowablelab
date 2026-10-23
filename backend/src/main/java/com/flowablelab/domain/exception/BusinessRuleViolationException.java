package com.flowablelab.domain.exception;

/** Violação de uma regra de negócio (dados inválidos, períodos conflitantes etc.). */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
