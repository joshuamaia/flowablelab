package com.flowablelab.infrastructure.persistence.repository;

import com.flowablelab.infrastructure.persistence.entity.VacationRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacationRecordJpaRepository extends JpaRepository<VacationRecordEntity, Long> {

    List<VacationRecordEntity> findByEmployeeIgnoreCase(String employee);

    boolean existsByRequestNumber(String requestNumber);
}
