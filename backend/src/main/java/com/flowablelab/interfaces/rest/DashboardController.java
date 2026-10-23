package com.flowablelab.interfaces.rest;

import com.flowablelab.application.usecase.DashboardUseCase;
import com.flowablelab.interfaces.rest.dto.Responses.DashboardSummaryResponse;
import com.flowablelab.interfaces.rest.dto.Responses.ProcessTypeResponse;
import com.flowablelab.interfaces.rest.dto.Responses.StatusCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores calculados a partir do Flowable e das solicitações")
public class DashboardController {

    private final DashboardUseCase dashboard;

    public DashboardController(DashboardUseCase dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    @Operation(summary = "Totais de processos, tarefas pendentes, aprovações e rejeições")
    public DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboard.summary());
    }

    @GetMapping("/process-status")
    @Operation(summary = "Distribuição das solicitações por status")
    public List<StatusCountResponse> processStatus() {
        return dashboard.statusDistribution().entrySet().stream()
                .map(entry -> new StatusCountResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @GetMapping("/process-types")
    @Operation(summary = "Instâncias em andamento e concluídas por tipo de processo")
    public List<ProcessTypeResponse> processTypes() {
        return dashboard.processTypes().stream().map(ProcessTypeResponse::from).toList();
    }
}
