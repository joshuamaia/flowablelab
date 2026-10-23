package com.flowablelab.domain.request;

import java.math.BigDecimal;
import java.util.Map;

import static com.flowablelab.domain.request.RequestData.optionalText;
import static com.flowablelab.domain.request.RequestData.requirePositive;
import static com.flowablelab.domain.request.RequestData.requireText;

public record PurchaseData(String requester, String description, BigDecimal amount, String justification)
        implements RequestData {

    /** A partir deste valor a validação automática marca a compra como prioridade alta. */
    public static final BigDecimal HIGH_PRIORITY_THRESHOLD = new BigDecimal("10000");

    public PurchaseData {
        requester = requireText(requester, "solicitante", 80);
        description = requireText(description, "descrição", 255);
        amount = requirePositive(amount, "valor");
        justification = optionalText(justification, "justificativa", 1000);
    }

    public String priority() {
        return amount.compareTo(HIGH_PRIORITY_THRESHOLD) >= 0 ? "ALTA" : "NORMAL";
    }

    @Override
    public RequestType type() {
        return RequestType.PURCHASE;
    }

    @Override
    public String summary() {
        return description;
    }

    @Override
    public Map<String, Object> toProcessVariables() {
        return Map.of(
                "requester", requester,
                "description", description,
                "amount", amount.doubleValue(),
                "justification", justification);
    }
}
