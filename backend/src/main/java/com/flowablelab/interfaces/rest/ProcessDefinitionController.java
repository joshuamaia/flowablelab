package com.flowablelab.interfaces.rest;

import com.flowablelab.application.usecase.ProcessQueryUseCase;
import com.flowablelab.interfaces.rest.dto.Responses.ProcessDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/process-definitions")
@Tag(name = "Definições de processos", description = "Definições BPMN implantadas no Flowable")
public class ProcessDefinitionController {

    private final ProcessQueryUseCase processes;

    public ProcessDefinitionController(ProcessQueryUseCase processes) {
        this.processes = processes;
    }

    @GetMapping
    @Operation(summary = "Lista a última versão de cada definição implantada")
    public List<ProcessDefinitionResponse> list() {
        return processes.definitions().stream().map(ProcessDefinitionResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma definição (id no formato chave:versão:deploymentId)")
    public ProcessDefinitionResponse get(@PathVariable String id) {
        return ProcessDefinitionResponse.from(processes.definition(id));
    }

    @GetMapping(value = "/{id}/diagram", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "XML BPMN 2.0 (com DI) da versão exata da definição")
    public String diagram(@PathVariable String id) {
        return processes.diagram(id);
    }
}
