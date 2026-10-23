package com.flowablelab.application.usecase;

import com.flowablelab.application.model.Views.StartedRequest;
import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.ProcessEnginePort.StartedProcess;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.domain.request.RequestData;
import com.flowablelab.domain.request.VacationData;
import com.flowablelab.domain.request.WorkflowRequest;
import com.flowablelab.domain.user.DemoUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Abre uma solicitação de negócio e inicia a instância BPMN correspondente.
 *
 * <p>Tudo ocorre na mesma transação: o Flowable e o JPA compartilham o DataSource e o
 * gerenciador de transações do Spring. Se uma Service Task síncrona falhar durante o
 * início do processo, a solicitação também é desfeita.</p>
 */
@Service
public class StartRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartRequestUseCase.class);

    private final WorkflowRequestRepository requests;
    private final ProcessEnginePort engine;
    private final TaskPort tasks;
    private final Clock clock;

    public StartRequestUseCase(WorkflowRequestRepository requests, ProcessEnginePort engine, TaskPort tasks,
                               Clock clock) {
        this.requests = requests;
        this.engine = engine;
        this.tasks = tasks;
        this.clock = clock;
    }

    @Transactional
    public StartedRequest start(RequestData data, DemoUser startedBy) {
        return start(data, startedBy, false);
    }

    @Transactional
    public StartedRequest start(RequestData data, DemoUser startedBy, boolean demo) {
        if (data instanceof VacationData vacation) {
            vacation.requireStartNotBefore(LocalDate.now(clock));
        }
        Instant now = clock.instant();
        String number = requests.nextRequestNumber();
        WorkflowRequest request = requests.save(WorkflowRequest.open(number, data, now, demo));

        Map<String, Object> variables = new HashMap<>(data.toProcessVariables());
        variables.put("requestNumber", number);
        variables.put("requestType", data.type().name());
        variables.put("startedBy", startedBy.id());

        String name = "%s · %s".formatted(number, data.summary());
        StartedProcess started = engine.start(data.type().processKey(), number, name, variables, startedBy.id());
        request.attachProcess(started.processInstanceId(), started.processDefinitionId());
        WorkflowRequest saved = requests.save(request);

        try (var ignored = MDC.putCloseable("processInstanceId", started.processInstanceId())) {
            log.info("Solicitação {} ({}) iniciada por {} na definição {}", number, data.type(), startedBy.id(),
                    started.processDefinitionId());
        }
        return new StartedRequest(saved, tasks.activeTasksOf(started.processInstanceId()));
    }
}
