package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Filters.ProcessInstanceFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.model.Views.DiagramState;
import com.flowablelab.application.model.Views.ProcessInstanceDetails;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ElementFailure;
import com.flowablelab.domain.workflow.ExecutionPath;
import com.flowablelab.domain.workflow.ProcessDefinitionInfo;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.ProcessVariable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consultas de definições, instâncias e estado de execução — tudo via APIs do motor. */
@Service
@Transactional(readOnly = true)
public class ProcessQueryUseCase {

    private final ProcessEnginePort engine;
    private final TaskPort tasks;
    private final WorkflowRequestRepository requests;

    public ProcessQueryUseCase(ProcessEnginePort engine, TaskPort tasks, WorkflowRequestRepository requests) {
        this.engine = engine;
        this.tasks = tasks;
        this.requests = requests;
    }

    public List<ProcessDefinitionInfo> definitions() {
        return engine.latestDefinitions();
    }

    public ProcessDefinitionInfo definition(String id) {
        return engine.findDefinition(id)
                .orElseThrow(() -> new ResourceNotFoundException("Definição de processo", id));
    }

    public String diagram(String definitionId) {
        definition(definitionId);
        return engine.definitionXml(definitionId);
    }

    public PageResult<ProcessInstanceInfo> instances(ProcessInstanceFilter filter, PageQuery page) {
        return engine.searchInstances(filter, page);
    }

    public ProcessInstanceInfo instance(String id) {
        return engine.findInstance(id).orElseThrow(() -> new ResourceNotFoundException("Instância de processo", id));
    }

    public ProcessInstanceDetails details(String id) {
        ProcessInstanceInfo instance = instance(id);
        return new ProcessInstanceDetails(instance,
                requests.findByProcessInstanceId(id).orElse(null),
                tasks.activeTasksOf(id),
                engine.failures(id));
    }

    public DiagramState diagramState(String id) {
        ProcessInstanceInfo instance = instance(id);
        List<ActivityExecution> executions = engine.activityExecutions(id);
        List<ElementFailure> failures = engine.failures(id);
        return new DiagramState(id, instance.processDefinitionId(), instance.ended(), executions,
                ExecutionPath.summarize(executions, failures,
                        instance.ended() ? List.of() : engine.waitingElementIds(id)));
    }

    public List<ProcessVariable> variables(String id) {
        instance(id);
        return engine.variables(id);
    }
}
