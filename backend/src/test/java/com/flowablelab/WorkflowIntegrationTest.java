package com.flowablelab;

import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.application.usecase.CompleteTaskUseCase;
import com.flowablelab.application.usecase.HistoryQueryUseCase;
import com.flowablelab.application.usecase.ProcessQueryUseCase;
import com.flowablelab.application.usecase.StartRequestUseCase;
import com.flowablelab.domain.exception.BusinessRuleViolationException;
import com.flowablelab.domain.exception.UserNotAllowedException;
import com.flowablelab.domain.request.AccessData;
import com.flowablelab.domain.request.AccessLevel;
import com.flowablelab.domain.request.ParallelAnalysisData;
import com.flowablelab.domain.request.PurchaseData;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.VacationData;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ActivityStatus;
import com.flowablelab.domain.workflow.ElementState;
import com.flowablelab.domain.workflow.WorkTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/** Execução real dos quatro processos BPMN no Flowable incorporado, com persistência em PostgreSQL. */
class WorkflowIntegrationTest extends IntegrationTestBase {

    @Autowired StartRequestUseCase startRequest;
    @Autowired CompleteTaskUseCase completeTask;
    @Autowired ProcessQueryUseCase processQuery;
    @Autowired HistoryQueryUseCase historyQuery;
    @Autowired WorkflowRequestRepository requests;
    @Autowired ProcessEnginePort engine;
    @Autowired TaskPort tasks;
    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired HistoryService historyService;
    @Autowired ManagementService managementService;

    private static String uniqueName() {
        return "Pessoa " + UUID.randomUUID().toString().substring(0, 8);
    }

    private WorkTask onlyTask(String processInstanceId, String taskKey) {
        return tasks.activeTasksOf(processInstanceId).stream()
                .filter(t -> t.taskDefinitionKey().equals(taskKey))
                .findFirst().orElseThrow(() -> new AssertionError("Tarefa ausente: " + taskKey));
    }

    private Map<String, ActivityStatus> elementStates(String processInstanceId) {
        return processQuery.diagramState(processInstanceId).elements().stream()
                .collect(Collectors.toMap(ElementState::elementId, ElementState::status));
    }

    @Test
    void osQuatroProcessosEstaoImplantadosComDiagramaGrafico() {
        assertThat(repositoryService.createProcessDefinitionQuery().latestVersion().list())
                .extracting("key")
                .contains("purchaseApproval", "vacationApproval", "accessRequest", "parallelApproval");
        var definition = engine.findLatestDefinitionByKey("parallelApproval").orElseThrow();
        assertThat(processQuery.diagram(definition.id())).contains("bpmndi:BPMNDiagram", "joinGateway");
    }

    @Test
    void compraAprovadaPercorreServiceTasksUserTaskEGateway() {
        var started = startRequest.start(new PurchaseData(uniqueName(), "Notebook", new BigDecimal("15000"), null),
                DemoUser.COLABORADOR);
        String pid = started.request().processInstanceId();

        // Service Tasks síncronas executadas e User Task criada para o grupo gestor
        WorkTask review = onlyTask(pid, "managerReview");
        assertThat(review.candidateGroups()).containsExactly("gestor");
        assertThat(review.processVariables()).containsEntry("validated", true).containsEntry("priority", "ALTA");
        assertThat(review.businessKey()).isEqualTo(started.request().number());

        var completion = completeTask.complete(review.id(), DemoUser.GESTOR, TaskAction.APPROVE, "ok");

        assertThat(completion.processEnded()).isTrue();
        var instance = engine.findInstance(pid).orElseThrow();
        assertThat(instance.endActivityId()).isEqualTo("endApproved");
        assertThat(requests.findByProcessInstanceId(pid).orElseThrow().status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pid).count()).isZero();
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(pid)
                .variableName("purchaseOrderNumber").singleResult().getValue())
                .isEqualTo(started.request().number().replace("REQ-", "PO-"));
    }

    @Test
    void compraRejeitadaSegueOutroRamoDoGatewayEDiagramaMostraCaminhoReal() {
        var started = startRequest.start(new PurchaseData(uniqueName(), "Cadeira", new BigDecimal("900"), null),
                DemoUser.COLABORADOR);
        String pid = started.request().processInstanceId();

        // Enquanto aguarda o gestor: a análise está ATIVA e nada depois dela foi marcado
        var waiting = elementStates(pid);
        assertThat(waiting).containsEntry("managerReview", ActivityStatus.ACTIVE)
                .containsEntry("automaticValidation", ActivityStatus.COMPLETED)
                .doesNotContainKeys("decisionGateway", "purchaseApproved", "purchaseRejected");

        completeTask.complete(onlyTask(pid, "managerReview").id(), DemoUser.GESTOR, TaskAction.REJECT, "Sem orçamento");

        var finished = elementStates(pid);
        assertThat(finished).containsEntry("flowRejected", ActivityStatus.COMPLETED)
                .containsEntry("endRejected", ActivityStatus.COMPLETED)
                .doesNotContainKeys("flowApproved", "purchaseApproved", "endApproved");
        assertThat(requests.findByProcessInstanceId(pid).orElseThrow().status()).isEqualTo(RequestStatus.REJECTED);

        // Histórico sem sequence flows, com responsável e decisão
        List<ActivityExecution> timeline = historyQuery.activities(pid);
        assertThat(timeline).noneMatch(ActivityExecution::isSequenceFlow);
        ActivityExecution review = timeline.stream().filter(a -> a.activityId().equals("managerReview")).findFirst()
                .orElseThrow();
        assertThat(review.assignee()).isEqualTo("gestor");
        assertThat(review.decision()).isEqualTo("REJECT");
        assertThat(review.comment()).isEqualTo("Sem orçamento");
    }

    @Test
    void falhaEmServiceTaskSincronaDesfazTudo() {
        long before = historyService.createHistoricProcessInstanceQuery().count();
        // Variáveis inválidas iniciadas diretamente no motor: a validação automática rejeita.
        assertThatThrownBy(() -> engine.start("purchaseApproval", "REQ-INEXISTENTE", "teste",
                Map.of("requestNumber", "REQ-INEXISTENTE", "requester", "x", "description", "y", "amount", -1.0),
                "colaborador"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(before);
    }

    @Test
    void feriasAprovadasSaoRegistradasNoRhPorServiceTaskAssincrona() {
        LocalDate start = LocalDate.now().plusDays(100);
        var started = startRequest.start(new VacationData(uniqueName(), start, start.plusDays(9), null),
                DemoUser.COLABORADOR);
        String pid = started.request().processInstanceId();
        completeTask.complete(onlyTask(pid, "managerReview").id(), DemoUser.GESTOR, TaskAction.APPROVE, null);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(requests.findByProcessInstanceId(pid).orElseThrow().status())
                        .isEqualTo(RequestStatus.APPROVED));
        assertThat(engine.findInstance(pid).orElseThrow().endActivityId()).isEqualTo("endApproved");
    }

    @Test
    void conflitoNoRhLevaJobParaDeadLetterEDestacaFalha() {
        String employee = uniqueName();
        LocalDate start = LocalDate.now().plusDays(200);
        for (int i = 0; i < 2; i++) {
            var started = startRequest.start(new VacationData(employee, start, start.plusDays(9), null),
                    DemoUser.COLABORADOR);
            completeTask.complete(onlyTask(started.request().processInstanceId(), "managerReview").id(),
                    DemoUser.GESTOR, TaskAction.APPROVE, null);
        }
        var second = requests.search(new com.flowablelab.application.model.Filters.RequestFilter(null, null, employee),
                com.flowablelab.application.model.PageQuery.of(0, 10)).content().getFirst();

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(requests.findById(second.id()).orElseThrow().status()).isEqualTo(RequestStatus.FAILED));
        String pid = second.processInstanceId();
        assertThat(managementService.createDeadLetterJobQuery().processInstanceId(pid).count()).isEqualTo(1);
        assertThat(elementStates(pid)).containsEntry("hrRegistration", ActivityStatus.FAILED);
        assertThat(requests.findById(second.id()).orElseThrow().statusDetail()).contains("Conflito no RH");
    }

    @Test
    void solicitacaoDeAcessoPassaPorEquipesDiferentes() {
        var started = startRequest.start(new AccessData(uniqueName(), "ERP", AccessLevel.ESCRITA, "Fechamento"),
                DemoUser.COLABORADOR);
        String pid = started.request().processInstanceId();
        var afterManager = completeTask.complete(onlyTask(pid, "managerReview").id(), DemoUser.GESTOR,
                TaskAction.APPROVE, null);

        WorkTask itTask = afterManager.nextTasks().getFirst();
        assertThat(itTask.taskDefinitionKey()).isEqualTo("itProvisioning");
        assertThat(itTask.candidateGroups()).containsExactly("ti");
        assertThatThrownBy(() -> completeTask.complete(itTask.id(), DemoUser.GESTOR, TaskAction.COMPLETE, null))
                .isInstanceOf(UserNotAllowedException.class);

        completeTask.complete(itTask.id(), DemoUser.TI, TaskAction.COMPLETE, "Usuário criado");

        assertThat(requests.findByProcessInstanceId(pid).orElseThrow().status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(historyQuery.tasks(pid)).extracting(WorkTask::assignee).containsExactly("gestor", "ti");
    }

    @Test
    void gatewayParaleloSoAvancaQuandoAsDuasAnalisesTerminam() {
        var started = startRequest.start(new ParallelAnalysisData(uniqueName(), "Nuvem", "Migrar",
                new BigDecimal("1000")), DemoUser.COLABORADOR);
        String pid = started.request().processInstanceId();
        var opened = completeTask.complete(onlyTask(pid, "openRequest").id(), DemoUser.COLABORADOR,
                TaskAction.COMPLETE, null);
        assertThat(opened.nextTasks()).extracting(WorkTask::taskDefinitionKey)
                .containsExactlyInAnyOrder("technicalAnalysis", "financialAnalysis");

        var afterTechnical = completeTask.complete(onlyTask(pid, "technicalAnalysis").id(), DemoUser.TI,
                TaskAction.APPROVE, null);
        assertThat(afterTechnical.processEnded()).isFalse();
        assertThat(afterTechnical.nextTasks()).extracting(WorkTask::taskDefinitionKey)
                .containsExactly("financialAnalysis");
        assertThat(elementStates(pid))
                .containsEntry("technicalAnalysis", ActivityStatus.COMPLETED)
                .containsEntry("financialAnalysis", ActivityStatus.ACTIVE)
                .containsEntry("joinGateway", ActivityStatus.ACTIVE)
                .doesNotContainKey("consolidateAnalyses");

        var afterFinancial = completeTask.complete(onlyTask(pid, "financialAnalysis").id(), DemoUser.GESTOR,
                TaskAction.REJECT, "Sem orçamento");
        assertThat(afterFinancial.processEnded()).isTrue();
        assertThat(elementStates(pid)).containsEntry("consolidateAnalyses", ActivityStatus.COMPLETED)
                .containsEntry("joinGateway", ActivityStatus.COMPLETED);
        assertThat(requests.findByProcessInstanceId(pid).orElseThrow().status()).isEqualTo(RequestStatus.REJECTED);
    }
}
