package com.flowablelab.interfaces.rest;

import com.flowablelab.application.usecase.HistoryQueryUseCase;
import com.flowablelab.interfaces.rest.dto.Responses.ActivityResponse;
import com.flowablelab.interfaces.rest.dto.Responses.ProcessInstanceDetailsResponse;
import com.flowablelab.interfaces.rest.dto.Responses.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history/processes")
@Tag(name = "Histórico", description = "Histórico de execução registrado pelo HistoryService do Flowable")
public class HistoryController {

    private final HistoryQueryUseCase history;

    public HistoryController(HistoryQueryUseCase history) {
        this.history = history;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Resumo histórico da instância (início, fim, duração, desfecho)")
    public ProcessInstanceDetailsResponse process(@PathVariable String id) {
        return ProcessInstanceDetailsResponse.from(history.process(id));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Timeline de atividades executadas, com responsáveis, decisões e durações")
    public List<ActivityResponse> activities(@PathVariable String id) {
        return history.activities(id).stream().map(ActivityResponse::from).toList();
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "Tarefas humanas (pendentes e concluídas) da instância")
    public List<TaskResponse> tasks(@PathVariable String id) {
        return history.tasks(id).stream().map(TaskResponse::from).toList();
    }
}
