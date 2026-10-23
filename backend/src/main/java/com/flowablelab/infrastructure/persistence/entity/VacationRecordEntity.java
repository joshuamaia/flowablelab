package com.flowablelab.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "hr_vacation_record")
public class VacationRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "request_number", nullable = false, unique = true)
    private String requestNumber;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected VacationRecordEntity() {
    }

    public VacationRecordEntity(String employee, LocalDate startDate, LocalDate endDate, String requestNumber,
                                Instant registeredAt) {
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requestNumber = requestNumber;
        this.registeredAt = registeredAt;
    }

    public String getEmployee() { return employee; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getRequestNumber() { return requestNumber; }
    public Instant getRegisteredAt() { return registeredAt; }
}
