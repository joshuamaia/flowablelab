package com.flowablelab;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: Spring Boot completo + Flowable incorporado + PostgreSQL
 * real em contêiner (Testcontainers). O contêiner é compartilhado entre as classes de teste.
 */
@SpringBootTest(properties = {
        "flowablelab.demo-data.enabled=false",
        // Acelera a reexecução de jobs com falha (retentativas agendadas como timer jobs).
        "flowable.process.async.executor.default-timer-job-acquire-wait-time=500ms",
        "flowable.process.async.executor.default-async-job-acquire-wait-time=500ms"
})
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6-alpine");

    static {
        POSTGRES.start();
    }
}
