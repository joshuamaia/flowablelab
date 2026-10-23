package com.flowablelab.domain.hr;

import java.time.Instant;
import java.time.LocalDate;

/** Registro local de férias efetuado pela Service Task "Registro RH". */
public record VacationRecord(String employee, LocalDate startDate, LocalDate endDate, String requestNumber,
                             Instant registeredAt) {

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }
}
