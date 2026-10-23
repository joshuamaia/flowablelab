package com.flowablelab.domain.request;

import com.flowablelab.domain.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Dados de abertura de uma solicitação. Cada implementação valida as próprias regras
 * de negócio no construtor e sabe gerar as variáveis que alimentam o processo BPMN.
 */
public sealed interface RequestData permits PurchaseData, VacationData, AccessData, ParallelAnalysisData {

    RequestType type();

    String requester();

    /** Texto curto exibido em listagens. */
    String summary();

    /** Valor monetário associado, quando existir. */
    default BigDecimal amount() {
        return null;
    }

    /** Variáveis de processo (somente tipos simples suportados nativamente pelo Flowable). */
    Map<String, Object> toProcessVariables();

    static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("O campo '%s' é obrigatório.".formatted(field));
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessRuleViolationException(
                    "O campo '%s' deve ter no máximo %d caracteres.".formatted(field, maxLength));
        }
        return trimmed;
    }

    static String optionalText(String value, String field, int maxLength) {
        return value == null || value.isBlank() ? "" : requireText(value, field, maxLength);
    }

    static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleViolationException("O campo '%s' deve ser maior que zero.".formatted(field));
        }
        return value;
    }
}
