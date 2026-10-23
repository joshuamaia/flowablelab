package com.flowablelab.infrastructure.persistence;

import com.flowablelab.application.port.out.VacationRecordRepository;
import com.flowablelab.domain.hr.VacationRecord;
import com.flowablelab.infrastructure.persistence.entity.VacationRecordEntity;
import com.flowablelab.infrastructure.persistence.repository.VacationRecordJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaVacationRecordRepository implements VacationRecordRepository {

    private final VacationRecordJpaRepository jpa;

    public JpaVacationRecordRepository(VacationRecordJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<VacationRecord> findByEmployee(String employee) {
        return jpa.findByEmployeeIgnoreCase(employee).stream()
                .map(e -> new VacationRecord(e.getEmployee(), e.getStartDate(), e.getEndDate(), e.getRequestNumber(),
                        e.getRegisteredAt()))
                .toList();
    }

    @Override
    public boolean existsByRequestNumber(String requestNumber) {
        return jpa.existsByRequestNumber(requestNumber);
    }

    @Override
    public void save(VacationRecord record) {
        jpa.save(new VacationRecordEntity(record.employee(), record.startDate(), record.endDate(),
                record.requestNumber(), record.registeredAt()));
    }
}
