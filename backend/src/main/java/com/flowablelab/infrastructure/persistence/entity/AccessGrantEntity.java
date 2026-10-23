package com.flowablelab.infrastructure.persistence.entity;

import com.flowablelab.domain.request.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "access_grant")
public class AccessGrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employee;

    @Column(name = "system_name", nullable = false)
    private String system;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel level;

    @Column(name = "request_number", nullable = false, unique = true)
    private String requestNumber;

    @Column(name = "provisioned_by")
    private String provisionedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected AccessGrantEntity() {
    }

    public AccessGrantEntity(String employee, String system, AccessLevel level, String requestNumber,
                             String provisionedBy, Instant grantedAt) {
        this.employee = employee;
        this.system = system;
        this.level = level;
        this.requestNumber = requestNumber;
        this.provisionedBy = provisionedBy;
        this.grantedAt = grantedAt;
    }
}
