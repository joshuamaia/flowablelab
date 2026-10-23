package com.flowablelab.domain.request;

import java.math.BigDecimal;
import java.util.Map;

import static com.flowablelab.domain.request.RequestData.requirePositive;
import static com.flowablelab.domain.request.RequestData.requireText;

public record ParallelAnalysisData(String requester, String title, String description, BigDecimal estimatedAmount)
        implements RequestData {

    public ParallelAnalysisData {
        requester = requireText(requester, "solicitante", 80);
        title = requireText(title, "título", 120);
        description = requireText(description, "descrição", 1000);
        estimatedAmount = requirePositive(estimatedAmount, "valor estimado");
    }

    @Override
    public RequestType type() {
        return RequestType.PARALLEL;
    }

    @Override
    public String summary() {
        return title;
    }

    @Override
    public BigDecimal amount() {
        return estimatedAmount;
    }

    @Override
    public Map<String, Object> toProcessVariables() {
        return Map.of(
                "requester", requester,
                "title", title,
                "description", description,
                "estimatedAmount", estimatedAmount.doubleValue());
    }
}
