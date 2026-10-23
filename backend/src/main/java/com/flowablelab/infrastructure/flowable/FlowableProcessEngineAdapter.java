package com.flowablelab.infrastructure.flowable;

import com.flowablelab.application.model.Filters.ProcessInstanceFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.model.PageResult;
import com.flowablelab.application.model.Views.ProcessTypeStats;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ElementFailure;
import com.flowablelab.domain.workflow.ProcessDefinitionInfo;
import com.flowablelab.domain.workflow.ProcessInstanceInfo;
import com.flowablelab.domain.workflow.ProcessVariable;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.JobInfo;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.flowablelab.infrastructure.flowable.FlowableMappers.instant;

/**
 * Adaptador da porta {@link ProcessEnginePort} que utiliza exclusivamente os serviços
 * oficiais do Flowable (RuntimeService, RepositoryService, HistoryService, ManagementService).
 * Nenhuma tabela ACT_* é consultada ou alterada diretamente.
 */
@Component
public class FlowableProcessEngineAdapter implements ProcessEnginePort {

    /** Tipos de elementos que não representam uma "etapa" para o usuário. */
    private static final Set<String> NON_STEP_TYPES = Set.of("sequenceFlow", "startEvent", "endEvent",
            "exclusiveGateway", "parallelGateway", "inclusiveGateway");

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final ManagementService managementService;
    private final TaskService taskService;
    private final IdentityService identityService;

    public FlowableProcessEngineAdapter(RuntimeService runtimeService, RepositoryService repositoryService,
                                        HistoryService historyService, ManagementService managementService,
                                        TaskService taskService, IdentityService identityService) {
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.historyService = historyService;
        this.managementService = managementService;
        this.taskService = taskService;
        this.identityService = identityService;
    }

    @Override
    public StartedProcess start(String processDefinitionKey, String businessKey, String name,
                                Map<String, Object> variables, String startUserId) {
        identityService.setAuthenticatedUserId(startUserId);
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(processDefinitionKey)
                    .businessKey(businessKey)
                    .name(name.length() > 255 ? name.substring(0, 255) : name)
                    .variables(variables)
                    .start();
            return new StartedProcess(instance.getId(), instance.getProcessDefinitionId());
        } catch (FlowableObjectNotFoundException e) {
            throw new ResourceNotFoundException("Definição de processo", processDefinitionKey);
        } catch (RuntimeException e) {
            throw FlowableExceptions.translate(e);
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    // ------------------------------------------------------------------ definições

    @Override
    public List<ProcessDefinitionInfo> latestDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionKey().asc()
                .list().stream()
                .map(FlowableMappers::definition)
                .toList();
    }

    @Override
    public Optional<ProcessDefinitionInfo> findDefinition(String processDefinitionId) {
        return Optional.ofNullable(repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(processDefinitionId)
                        .singleResult())
                .map(FlowableMappers::definition);
    }

    @Override
    public Optional<ProcessDefinitionInfo> findLatestDefinitionByKey(String processDefinitionKey) {
        return Optional.ofNullable(repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(processDefinitionKey)
                        .latestVersion()
                        .singleResult())
                .map(FlowableMappers::definition);
    }

    @Override
    public String definitionXml(String processDefinitionId) {
        ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);
        try (InputStream xml = repositoryService.getResourceAsStream(definition.getDeploymentId(),
                definition.getResourceName())) {
            return new String(xml.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ instâncias

    @Override
    public Optional<ProcessInstanceInfo> findInstance(String processInstanceId) {
        return Optional.ofNullable(historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult())
                .map(FlowableMappers::instance);
    }

    @Override
    public PageResult<ProcessInstanceInfo> searchInstances(ProcessInstanceFilter filter, PageQuery page) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if (filter.processDefinitionKey() != null && !filter.processDefinitionKey().isBlank()) {
            query.processDefinitionKey(filter.processDefinitionKey());
        }
        if (Boolean.TRUE.equals(filter.finished())) {
            query.finished();
        } else if (Boolean.FALSE.equals(filter.finished())) {
            query.unfinished();
        }
        if (filter.businessKey() != null && !filter.businessKey().isBlank()) {
            query.processInstanceBusinessKeyLike("%" + filter.businessKey().trim() + "%");
        }
        long total = query.count();
        query.orderByProcessInstanceStartTime();
        if (page.ascending()) {
            query.asc();
        } else {
            query.desc();
        }
        List<ProcessInstanceInfo> content = query.listPage(page.offset(), page.size()).stream()
                .map(FlowableMappers::instance)
                .toList();
        return new PageResult<>(content, page.page(), page.size(), total);
    }

    @Override
    public List<ActivityExecution> activityExecutions(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list().stream()
                .sorted(Comparator.comparing(HistoricActivityInstance::getStartTime)
                        .thenComparing(a -> a.getTransactionOrder() == null ? 0 : a.getTransactionOrder()))
                .map(a -> new ActivityExecution(a.getId(), a.getActivityId(), a.getActivityName(),
                        a.getActivityType(), a.getExecutionId(), a.getTaskId(), a.getAssignee(),
                        instant(a.getStartTime()), instant(a.getEndTime()), a.getDurationInMillis(), null, null))
                .toList();
    }

    @Override
    public List<ElementFailure> failures(String processInstanceId) {
        Stream<? extends JobInfo> deadLetters = managementService.createDeadLetterJobQuery()
                .processInstanceId(processInstanceId).list().stream();
        Stream<? extends JobInfo> retrying = managementService.createTimerJobQuery()
                .processInstanceId(processInstanceId).withException().list().stream();
        Stream<? extends JobInfo> executable = managementService.createJobQuery()
                .processInstanceId(processInstanceId).withException().list().stream();
        return Stream.of(deadLetters, retrying, executable)
                .flatMap(s -> s)
                .filter(job -> job instanceof org.flowable.job.api.Job)
                .map(job -> (org.flowable.job.api.Job) job)
                .filter(job -> job.getElementId() != null)
                .map(job -> new ElementFailure(job.getElementId(), job.getExceptionMessage()))
                .toList();
    }

    @Override
    public List<String> waitingElementIds(String processInstanceId) {
        return runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .onlyChildExecutions()
                .list().stream()
                .map(Execution::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public List<ProcessVariable> variables(String processInstanceId) {
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .excludeTaskVariables()
                .orderByVariableName().asc()
                .list().stream()
                .map(v -> new ProcessVariable(v.getVariableName(), v.getVariableTypeName(),
                        FlowableMappers.safeValue(v.getValue()), instant(v.getLastUpdatedTime())))
                .toList();
    }

    @Override
    public Map<String, List<String>> currentStepNames(Collection<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> steps = new LinkedHashMap<>();
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceIdIn(processInstanceIds)
                .orderByTaskCreateTime().asc()
                .list();
        tasks.forEach(task -> steps.computeIfAbsent(task.getProcessInstanceId(), k -> new ArrayList<>())
                .add(task.getName()));

        // Instâncias sem User Task pendente podem estar aguardando um job assíncrono.
        Set<String> running = runtimeService.createProcessInstanceQuery()
                .processInstanceIds(Set.copyOf(processInstanceIds))
                .list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toSet());
        running.stream()
                .filter(id -> !steps.containsKey(id))
                .forEach(id -> {
                    List<String> names = runtimeService.createActivityInstanceQuery()
                            .processInstanceId(id)
                            .unfinished()
                            .list().stream()
                            .filter(a -> !NON_STEP_TYPES.contains(a.getActivityType()))
                            .map(ActivityInstance::getActivityName)
                            .filter(n -> n != null && !n.isBlank())
                            .distinct()
                            .toList();
                    if (!names.isEmpty()) {
                        steps.put(id, names);
                    }
                });
        return steps;
    }

    // ------------------------------------------------------------------ indicadores

    @Override
    public long countInstances(Boolean finished) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if (Boolean.TRUE.equals(finished)) {
            query.finished();
        } else if (Boolean.FALSE.equals(finished)) {
            query.unfinished();
        }
        return query.count();
    }

    @Override
    public List<ProcessTypeStats> statsByDefinition() {
        return latestDefinitions().stream()
                .map(definition -> new ProcessTypeStats(definition.key(), definition.name(),
                        historyService.createHistoricProcessInstanceQuery()
                                .processDefinitionKey(definition.key()).unfinished().count(),
                        historyService.createHistoricProcessInstanceQuery()
                                .processDefinitionKey(definition.key()).finished().count()))
                .toList();
    }
}
