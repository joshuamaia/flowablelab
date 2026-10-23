package com.flowablelab.application.usecase;

import com.flowablelab.application.port.out.VacationRecordRepository;
import com.flowablelab.domain.exception.BusinessRuleViolationException;
import com.flowablelab.domain.hr.VacationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** Registro local de férias no "sistema de RH" (tabela de negócio do próprio FlowableLab). */
@Service
public class RegisterVacationUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterVacationUseCase.class);

    private final VacationRecordRepository records;
    private final Clock clock;

    public RegisterVacationUseCase(VacationRecordRepository records, Clock clock) {
        this.records = records;
        this.clock = clock;
    }

    /**
     * Idempotente por número de solicitação (o job assíncrono pode ser reexecutado).
     *
     * @throws BusinessRuleViolationException se já houver férias registradas no período
     */
    @Transactional
    public void register(String requestNumber, String employee, LocalDate startDate, LocalDate endDate) {
        if (records.existsByRequestNumber(requestNumber)) {
            log.info("Férias da solicitação {} já registradas no RH", requestNumber);
            return;
        }
        records.findByEmployee(employee).stream()
                .filter(existing -> existing.overlaps(startDate, endDate))
                .findFirst()
                .ifPresent(conflict -> {
                    throw new BusinessRuleViolationException(
                            "Conflito no RH: %s já possui férias registradas de %s a %s (solicitação %s)."
                                    .formatted(employee, conflict.startDate(), conflict.endDate(),
                                            conflict.requestNumber()));
                });
        records.save(new VacationRecord(employee, startDate, endDate, requestNumber, clock.instant()));
        log.info("Férias de {} ({} a {}) registradas no RH para a solicitação {}", employee, startDate, endDate,
                requestNumber);
    }
}
