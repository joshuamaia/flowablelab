package com.flowablelab.interfaces.rest.dto;

import com.flowablelab.domain.task.TaskAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteTaskRequest(
        @Schema(description = "APPROVE, REJECT ou COMPLETE, conforme a tarefa") @NotNull TaskAction action,
        @Schema(description = "Comentário/justificativa (obrigatório para rejeições)", example = "Orçamento aprovado")
        @Size(max = 1000) String comment) {
}
