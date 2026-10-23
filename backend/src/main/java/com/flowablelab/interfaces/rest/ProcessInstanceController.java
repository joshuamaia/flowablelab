package com.flowablelab.interfaces.rest;

import com.flowablelab.application.model.Filters.ProcessInstanceFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.Views.StartedRequest;
import com.flowablelab.application.usecase.ProcessQueryUseCase;
import com.flowablelab.application.usecase.StartRequestUseCase;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.interfaces.rest.dto.Responses.DiagramStateResponse;
import com.flowablelab.interfaces.rest.dto.Responses.PageResponse;
import com.flowablelab.interfaces.rest.dto.Responses.ProcessInstanceDetailsResponse;
import com.flowablelab.interfaces.rest.dto.Responses.ProcessInstanceResponse;
import com.flowablelab.interfaces.rest.dto.Responses.RequestResponse;
import com.flowablelab.interfaces.rest.dto.Responses.StartProcessResponse;
import com.flowablelab.interfaces.rest.dto.Responses.TaskResponse;
import com.flowablelab.interfaces.rest.dto.Responses.VariableResponse;
import com.flowablelab.interfaces.rest.dto.StartProcessRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/process-instances")
@Tag(name = "Instâncias de processos", description = "Início e consulta de instâncias executadas pelo Flowable")
public class ProcessInstanceController {

    private final StartRequestUseCase startRequest;
    private final ProcessQueryUseCase processes;

    public ProcessInstanceController(StartRequestUseCase startRequest, ProcessQueryUseCase processes) {
        this.startRequest = startRequest;
        this.processes = processes;
    }

    @PostMapping
    @Operation(summary = "Abre uma solicitação e inicia o processo BPMN pela chave da definição",
            description = "O número da solicitação é usado como businessKey da instância.")
    public ResponseEntity<StartProcessResponse> start(
            @Valid @RequestBody StartProcessRequest body,
            @Parameter(description = DemoUserHeader.DESCRIPTION)
            @RequestHeader(name = DemoUserHeader.NAME, defaultValue = "colaborador") String user) {
        StartedRequest started = startRequest.start(body.toDomain(), DemoUser.fromId(user));
        List<TaskResponse> tasks = started.activeTasks().stream().map(TaskResponse::from).toList();
        StartProcessResponse response = new StartProcessResponse(
                RequestResponse.from(started.request(), tasks.stream().map(TaskResponse::name).toList()), tasks);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(started.request().processInstanceId()).toUri())
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Lista instâncias (em andamento e finalizadas) com paginação e filtros")
    public PageResponse<ProcessInstanceResponse> list(
            @RequestParam(required = false) String processKey,
            @Parameter(description = "true = finalizadas, false = em andamento, vazio = todas")
            @RequestParam(required = false) Boolean finished,
            @RequestParam(required = false) String businessKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String direction) {
        return PageResponse.of(processes.instances(new ProcessInstanceFilter(processKey, finished, businessKey),
                new PageQuery(page, size, "startTime", "asc".equalsIgnoreCase(direction))),
                ProcessInstanceResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes da instância: dados gerais, solicitação, tarefas ativas e falhas")
    public ProcessInstanceDetailsResponse get(@PathVariable String id) {
        return ProcessInstanceDetailsResponse.from(processes.details(id));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Caminho real de execução e estado de cada elemento BPMN (para o visualizador)")
    public DiagramStateResponse activities(@PathVariable String id) {
        return DiagramStateResponse.from(processes.diagramState(id));
    }

    @GetMapping("/{id}/variables")
    @Operation(summary = "Variáveis de processo (valores atuais ou finais)")
    public List<VariableResponse> variables(@PathVariable String id) {
        return processes.variables(id).stream().map(VariableResponse::from).toList();
    }
}
