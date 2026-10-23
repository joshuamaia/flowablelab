-- Tabelas de negócio do FlowableLab.
-- Ficam no schema "business", separadas das tabelas internas do Flowable (ACT_*, schema public),
-- que são criadas e versionadas exclusivamente pelo próprio motor.

CREATE SEQUENCE workflow_request_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE workflow_request (
    id                    UUID          PRIMARY KEY,
    request_number        VARCHAR(30)   NOT NULL UNIQUE,
    request_type          VARCHAR(20)   NOT NULL,
    requester             VARCHAR(80)   NOT NULL,
    summary               VARCHAR(255)  NOT NULL,
    amount                NUMERIC(15, 2),
    status                VARCHAR(20)   NOT NULL,
    status_detail         VARCHAR(1000),
    process_instance_id   VARCHAR(64)   UNIQUE,
    process_definition_id VARCHAR(255),
    opened_at             TIMESTAMPTZ   NOT NULL,
    finished_at           TIMESTAMPTZ,
    demo                  BOOLEAN       NOT NULL DEFAULT FALSE,
    version               BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_workflow_request_status ON workflow_request (status);
CREATE INDEX idx_workflow_request_type ON workflow_request (request_type);
CREATE INDEX idx_workflow_request_opened_at ON workflow_request (opened_at DESC);

-- "Sistema de RH" local alimentado pela Service Task de registro de férias.
CREATE TABLE hr_vacation_record (
    id             BIGSERIAL   PRIMARY KEY,
    employee       VARCHAR(80) NOT NULL,
    start_date     DATE        NOT NULL,
    end_date       DATE        NOT NULL,
    request_number VARCHAR(30) NOT NULL UNIQUE,
    registered_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_hr_vacation_employee ON hr_vacation_record (employee);

-- Concessões de acesso registradas pela Service Task "Conceder acesso".
CREATE TABLE access_grant (
    id             BIGSERIAL    PRIMARY KEY,
    employee       VARCHAR(80)  NOT NULL,
    system_name    VARCHAR(120) NOT NULL,
    access_level   VARCHAR(20)  NOT NULL,
    request_number VARCHAR(30)  NOT NULL UNIQUE,
    provisioned_by VARCHAR(40),
    granted_at     TIMESTAMPTZ  NOT NULL
);
