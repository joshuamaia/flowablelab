package com.flowablelab.application;

import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.application.port.out.ProcessEnginePort.StartedProcess;
import com.flowablelab.application.port.out.TaskPort;
import com.flowablelab.application.port.out.VacationRecordRepository;
import com.flowablelab.application.port.out.WorkflowRequestRepository;
import com.flowablelab.application.usecase.RegisterVacationUseCase;
import com.flowablelab.application.usecase.StartRequestUseCase;
import com.flowablelab.domain.exception.BusinessRuleViolationException;
import com.flowablelab.domain.hr.VacationRecord;
import com.flowablelab.domain.request.PurchaseData;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.VacationData;
import com.flowablelab.domain.request.WorkflowRequest;
import com.flowablelab.domain.user.DemoUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StartRequestUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private final WorkflowRequestRepository requests = mock(WorkflowRequestRepository.class);
    private final ProcessEnginePort engine = mock(ProcessEnginePort.class);
    private final TaskPort tasks = mock(TaskPort.class);
    private final StartRequestUseCase useCase = new StartRequestUseCase(requests, engine, tasks, clock);

    @Test
    @SuppressWarnings("unchecked")
    void iniciaProcessoComBusinessKeyEVariaveis() {
        when(requests.nextRequestNumber()).thenReturn("REQ-2026-000007");
        when(requests.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(engine.start(anyString(), anyString(), anyString(), anyMap(), anyString()))
                .thenReturn(new StartedProcess("pi-7", "purchaseApproval:1:x"));
        when(tasks.activeTasksOf("pi-7")).thenReturn(List.of());

        var started = useCase.start(new PurchaseData("Ana", "Notebook", new BigDecimal("2500"), null), DemoUser.COLABORADOR);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(engine).start(eq("purchaseApproval"), eq("REQ-2026-000007"), anyString(), variables.capture(),
                eq("colaborador"));
        assertThat(variables.getValue()).containsEntry("requestNumber", "REQ-2026-000007")
                .containsEntry("amount", 2500.0).containsEntry("startedBy", "colaborador");
        WorkflowRequest request = started.request();
        assertThat(request.processInstanceId()).isEqualTo("pi-7");
        assertThat(request.status()).isEqualTo(RequestStatus.IN_PROGRESS);
    }

    @Test
    void feriasNoPassadoSaoRejeitadasAntesDeTocarNoMotor() {
        var past = LocalDate.of(2026, 5, 1);
        assertThatThrownBy(() -> useCase.start(new VacationData("Bruno", past, past.plusDays(9), null), DemoUser.COLABORADOR))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(engine);
    }

    @Test
    void registroRhDetectaConflitoEEhIdempotente() {
        VacationRecordRepository records = mock(VacationRecordRepository.class);
        var register = new RegisterVacationUseCase(records, clock);
        var start = LocalDate.of(2026, 7, 1);
        when(records.findByEmployee("Bruno")).thenReturn(List.of(
                new VacationRecord("Bruno", start, start.plusDays(9), "REQ-1", Instant.now())));

        assertThatThrownBy(() -> register.register("REQ-2", "Bruno", start.plusDays(5), start.plusDays(15)))
                .hasMessageContaining("Conflito no RH");

        when(records.existsByRequestNumber("REQ-1")).thenReturn(true);
        register.register("REQ-1", "Bruno", start, start.plusDays(9));
        verify(records, never()).save(any());
    }
}
