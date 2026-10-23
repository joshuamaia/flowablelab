package com.flowablelab.infrastructure.demo;

import com.flowablelab.application.model.Views.StartedRequest;
import com.flowablelab.application.model.Views.TaskCompletion;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.application.usecase.CompleteTaskUseCase;
import com.flowablelab.application.usecase.StartRequestUseCase;
import com.flowablelab.domain.request.AccessData;
import com.flowablelab.domain.request.AccessLevel;
import com.flowablelab.domain.request.ParallelAnalysisData;
import com.flowablelab.domain.request.PurchaseData;
import com.flowablelab.domain.request.RequestData;
import com.flowablelab.domain.request.VacationData;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.WorkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga inicial de exemplos. Tudo é criado executando os processos reais pelo Flowable
 * (mesmos casos de uso da API) — nada é inserido diretamente nas tabelas.
 *
 * <p>Idempotente: se já existir alguma solicitação marcada como demonstrativa, nada é feito.
 * A carga ocorre em uma única transação, portanto não fica parcialmente aplicada.</p>
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "flowablelab.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final StartRequestUseCase startRequest;
    private final CompleteTaskUseCase completeTask;
    private final WorkflowRequestRepository requests;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public DemoDataSeeder(StartRequestUseCase startRequest, CompleteTaskUseCase completeTask,
                          WorkflowRequestRepository requests, TransactionTemplate transaction, Clock clock) {
        this.startRequest = startRequest;
        this.completeTask = completeTask;
        this.requests = requests;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (requests.existsDemoData()) {
            log.info("Dados demonstrativos já existentes — carga inicial ignorada");
            return;
        }
        transaction.executeWithoutResult(status -> seed());
        log.info("Dados demonstrativos criados com sucesso");
    }

    private void seed() {
        LocalDate today = LocalDate.now(clock);

        // 1. Compra aguardando aprovação do gestor
        start(new PurchaseData("Ana Souza", "Notebooks para a equipe de dados", new BigDecimal("18500.00"),
                "Renovação do parque de equipamentos"));

        // 2. Compra aprovada
        StartedRequest approved = start(new PurchaseData("Rafael Torres", "Licenças de IDE", new BigDecimal("3200.00"),
                "Ferramentas de desenvolvimento"));
        complete(approved.activeTasks(), "managerReview", DemoUser.GESTOR, TaskAction.APPROVE,
                "Dentro do orçamento do trimestre.");

        // 3. Compra rejeitada (rejeição exige justificativa)
        StartedRequest rejected = start(new PurchaseData("Marina Alves", "Cadeiras gamer", new BigDecimal("9800.00"),
                "Conforto da equipe"));
        complete(rejected.activeTasks(), "managerReview", DemoUser.GESTOR, TaskAction.REJECT,
                "Item fora da política de mobiliário.");

        // 4. Férias pendentes de análise
        start(new VacationData("Bruno Lima", today.plusDays(30), today.plusDays(44), "Viagem em família"));

        // 5. Férias aprovadas — o Registro RH (assíncrono) conclui o processo logo após o commit
        StartedRequest vacation = start(new VacationData("Juliana Prado", today.plusDays(60), today.plusDays(69),
                "Descanso"));
        complete(vacation.activeTasks(), "managerReview", DemoUser.GESTOR, TaskAction.APPROVE, "Boas férias!");

        // 6. Acesso aguardando a equipe de TI
        StartedRequest access = start(new AccessData("Carla Mendes", "ERP Financeiro", AccessLevel.ESCRITA,
                "Fechamento contábil mensal"));
        complete(access.activeTasks(), "managerReview", DemoUser.GESTOR, TaskAction.APPROVE,
                "Necessário para o fechamento.");

        // 7. Acesso concluído (gestor + TI + concessão automática)
        StartedRequest granted = start(new AccessData("Pedro Nunes", "Portal de BI", AccessLevel.LEITURA,
                "Acompanhamento de indicadores"));
        TaskCompletion afterManager = complete(granted.activeTasks(), "managerReview", DemoUser.GESTOR,
                TaskAction.APPROVE, "Aprovado.");
        complete(afterManager.nextTasks(), "itProvisioning", DemoUser.TI, TaskAction.COMPLETE,
                "Usuário criado no grupo BI-Leitura.");

        // 8. Análises paralelas em andamento: técnica concluída, financeira pendente
        StartedRequest parallel = start(new ParallelAnalysisData("Diego Rocha", "Migração para nuvem",
                "Migrar o ambiente de homologação para a nuvem pública.", new BigDecimal("75000.00")));
        TaskCompletion opened = complete(parallel.activeTasks(), "openRequest", DemoUser.COLABORADOR,
                TaskAction.COMPLETE, "Enviado para análise.");
        complete(opened.nextTasks(), "technicalAnalysis", DemoUser.TI, TaskAction.APPROVE,
                "Arquitetura validada.");
    }

    private StartedRequest start(RequestData data) {
        return startRequest.start(data, DemoUser.COLABORADOR, true);
    }

    private TaskCompletion complete(List<WorkTask> tasks, String taskKey, DemoUser user, TaskAction action,
                                    String comment) {
        WorkTask task = tasks.stream()
                .filter(t -> t.taskDefinitionKey().equals(taskKey))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tarefa demonstrativa não encontrada: " + taskKey));
        return completeTask.complete(task.id(), user, action, comment);
    }
}
