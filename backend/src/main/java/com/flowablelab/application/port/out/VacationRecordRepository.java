package com.flowablelab.application.port.out;

import com.flowablelab.domain.hr.VacationRecord;

import java.util.List;

public interface VacationRecordRepository {

    List<VacationRecord> findByEmployee(String employee);

    boolean existsByRequestNumber(String requestNumber);

    void save(VacationRecord record);
}
