package com.flowablelab.domain.request;

import com.flowablelab.domain.exception.BusinessRuleViolationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.flowablelab.domain.request.RequestData.optionalText;
import static com.flowablelab.domain.request.RequestData.requireText;

public record VacationData(String requester, LocalDate startDate, LocalDate endDate, String notes)
        implements RequestData {

    public static final int MIN_DAYS = 5;
    public static final int MAX_DAYS = 30;

    public VacationData {
        requester = requireText(requester, "colaborador", 80);
        notes = optionalText(notes, "observações", 500);
        if (startDate == null || endDate == null) {
            throw new BusinessRuleViolationException("As datas inicial e final são obrigatórias.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleViolationException("A data final não pode ser anterior à data inicial.");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new BusinessRuleViolationException(
                    "O período de férias deve ter entre %d e %d dias (informado: %d)."
                            .formatted(MIN_DAYS, MAX_DAYS, days));
        }
    }

    /** Regra aplicada na abertura de novas solicitações. */
    public void requireStartNotBefore(LocalDate today) {
        if (startDate.isBefore(today)) {
            throw new BusinessRuleViolationException("A data inicial das férias não pode estar no passado.");
        }
    }

    public long days() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    @Override
    public RequestType type() {
        return RequestType.VACATION;
    }

    @Override
    public String summary() {
        return "Férias de %s a %s (%d dias)".formatted(startDate, endDate, days());
    }

    @Override
    public Map<String, Object> toProcessVariables() {
        return Map.of(
                "requester", requester,
                "startDate", startDate,
                "endDate", endDate,
                "days", (int) days(),
                "notes", notes);
    }
}
