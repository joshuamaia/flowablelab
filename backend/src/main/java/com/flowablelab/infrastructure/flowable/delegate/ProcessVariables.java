package com.flowablelab.infrastructure.flowable.delegate;

import com.flowablelab.domain.exception.BusinessRuleViolationException;
import org.flowable.engine.delegate.DelegateExecution;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Leitura tipada de variáveis de processo pelos delegates. */
final class ProcessVariables {

    private ProcessVariables() {
    }

    static String string(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value == null ? null : value.toString();
    }

    static String requiredString(DelegateExecution execution, String name) {
        String value = string(execution, name);
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("Variável de processo obrigatória ausente: " + name);
        }
        return value;
    }

    static BigDecimal decimal(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value instanceof Number number ? new BigDecimal(number.toString()) : null;
    }

    static LocalDate date(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof LocalDate date) {
            return date;
        }
        return value == null ? null : LocalDate.parse(value.toString());
    }

    static boolean bool(DelegateExecution execution, String name) {
        return Boolean.TRUE.equals(execution.getVariable(name));
    }
}
