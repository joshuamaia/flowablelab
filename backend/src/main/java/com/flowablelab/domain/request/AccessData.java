package com.flowablelab.domain.request;

import com.flowablelab.domain.exception.BusinessRuleViolationException;

import java.util.Map;

import static com.flowablelab.domain.request.RequestData.requireText;

public record AccessData(String requester, String system, AccessLevel accessLevel, String justification)
        implements RequestData {

    public AccessData {
        requester = requireText(requester, "solicitante", 80);
        system = requireText(system, "sistema", 120);
        justification = requireText(justification, "justificativa", 1000);
        if (accessLevel == null) {
            throw new BusinessRuleViolationException("O campo 'nível de acesso' é obrigatório.");
        }
    }

    @Override
    public RequestType type() {
        return RequestType.ACCESS;
    }

    @Override
    public String summary() {
        return "Acesso %s ao sistema %s".formatted(accessLevel.name().toLowerCase(), system);
    }

    @Override
    public Map<String, Object> toProcessVariables() {
        return Map.of(
                "requester", requester,
                "system", system,
                "accessLevel", accessLevel.name(),
                "justification", justification);
    }
}
