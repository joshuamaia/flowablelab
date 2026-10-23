package com.flowablelab.application.port.out;

import com.flowablelab.application.model.Filters.ProcessInstanceFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.model.Views.ProcessTypeStats;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ElementFailure;
import com.flowablelab.domain.workflow.ProcessDefinitionInfo;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.ProcessVariable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Porta de saída para o motor BPMN (implementada pelo adaptador Flowable). */
public interface ProcessEnginePort {

    record StartedProcess(String processInstanceId, String processDefinitionId) {
    }

    StartedProcess start(String processDefinitionKey, String businessKey, String name,
                         Map<String, Object> variables, String startUserId);

    // ----- definições -----

    List<ProcessDefinitionInfo> latestDefinitions();

    Optional<ProcessDefinitionInfo> findDefinition(String processDefinitionId);

    Optional<ProcessDefinitionInfo> findLatestDefinitionByKey(String processDefinitionKey);

    /** XML BPMN exato da versão implantada da definição. */
    String definitionXml(String processDefinitionId);

    // ----- instâncias -----

    Optional<ProcessInstanceInfo> findInstance(String processInstanceId);

    PageResult<ProcessInstanceInfo> searchInstances(ProcessInstanceFilter filter, PageQuery page);

    /** Histórico de execução ordenado (inclui sequence flows e gateways). */
    List<ActivityExecution> activityExecutions(String processInstanceId);

    List<ElementFailure> failures(String processInstanceId);

    /** Elementos em que existem execuções em espera no runtime (tarefas, jobs, junções paralelas). */
    List<String> waitingElementIds(String processInstanceId);

    List<ProcessVariable> variables(String processInstanceId);

    /** Nomes das atividades em espera (User Tasks ou jobs) por instância. */
    Map<String, List<String>> currentStepNames(Collection<String> processInstanceIds);

    // ----- indicadores -----

    long countInstances(Boolean finished);

    List<ProcessTypeStats> statsByDefinition();
}
