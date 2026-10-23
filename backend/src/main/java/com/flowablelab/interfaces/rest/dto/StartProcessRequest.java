package com.flowablelab.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.flowablelab.domain.request.AccessData;
import com.flowablelab.domain.request.AccessLevel;
import com.flowablelab.domain.request.ParallelAnalysisData;
import com.flowablelab.domain.request.PurchaseData;
import com.flowablelab.domain.request.RequestData;
import com.flowablelab.domain.request.VacationData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corpo de {@code POST /api/process-instances}. O campo {@code processKey} identifica a
 * definição BPMN e determina quais dados de negócio são exigidos.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "processKey",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartProcessRequest.Purchase.class, name = "purchaseApproval"),
        @JsonSubTypes.Type(value = StartProcessRequest.Vacation.class, name = "vacationApproval"),
        @JsonSubTypes.Type(value = StartProcessRequest.Access.class, name = "accessRequest"),
        @JsonSubTypes.Type(value = StartProcessRequest.Parallel.class, name = "parallelApproval")
})
@Schema(description = "Dados para iniciar um processo", oneOf = {
        StartProcessRequest.Purchase.class, StartProcessRequest.Vacation.class,
        StartProcessRequest.Access.class, StartProcessRequest.Parallel.class})
public sealed interface StartProcessRequest {

    String processKey();

    RequestData toDomain();

    @Schema(name = "PurchaseStartRequest")
    record Purchase(
            @Schema(example = "purchaseApproval") @NotBlank String processKey,
            @Schema(example = "Ana Souza") @NotBlank @Size(max = 80) String requester,
            @Schema(example = "Notebooks para a equipe de dados") @NotBlank @Size(max = 255) String description,
            @Schema(example = "18500.00") @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @Schema(example = "Renovação do parque de equipamentos") @Size(max = 1000) String justification)
            implements StartProcessRequest {

        @Override
        public RequestData toDomain() {
            return new PurchaseData(requester, description, amount, justification);
        }
    }

    @Schema(name = "VacationStartRequest")
    record Vacation(
            @Schema(example = "vacationApproval") @NotBlank String processKey,
            @Schema(example = "Bruno Lima") @NotBlank @Size(max = 80) String requester,
            @Schema(example = "2026-12-01") @NotNull LocalDate startDate,
            @Schema(example = "2026-12-15") @NotNull LocalDate endDate,
            @Schema(example = "Viagem em família") @Size(max = 500) String notes)
            implements StartProcessRequest {

        @Override
        public RequestData toDomain() {
            return new VacationData(requester, startDate, endDate, notes);
        }
    }

    @Schema(name = "AccessStartRequest")
    record Access(
            @Schema(example = "accessRequest") @NotBlank String processKey,
            @Schema(example = "Carla Mendes") @NotBlank @Size(max = 80) String requester,
            @Schema(example = "ERP Financeiro") @NotBlank @Size(max = 120) String system,
            @NotNull AccessLevel accessLevel,
            @Schema(example = "Fechamento contábil mensal") @NotBlank @Size(max = 1000) String justification)
            implements StartProcessRequest {

        @Override
        public RequestData toDomain() {
            return new AccessData(requester, system, accessLevel, justification);
        }
    }

    @Schema(name = "ParallelStartRequest")
    record Parallel(
            @Schema(example = "parallelApproval") @NotBlank String processKey,
            @Schema(example = "Diego Rocha") @NotBlank @Size(max = 80) String requester,
            @Schema(example = "Migração para nuvem") @NotBlank @Size(max = 120) String title,
            @Schema(example = "Migrar o ambiente de homologação") @NotBlank @Size(max = 1000) String description,
            @Schema(example = "75000.00") @NotNull @Positive @Digits(integer = 13, fraction = 2)
            BigDecimal estimatedAmount)
            implements StartProcessRequest {

        @Override
        public RequestData toDomain() {
            return new ParallelAnalysisData(requester, title, description, estimatedAmount);
        }
    }
}
