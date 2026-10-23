package com.flowablelab.domain;

import com.flowablelab.domain.exception.BusinessRuleViolationException;
import com.flowablelab.domain.request.PurchaseData;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.WorkflowRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowRequestTest {

    private final WorkflowRequest request = WorkflowRequest.open("REQ-2026-000001",
            new PurchaseData("Ana", "Notebook", BigDecimal.TEN, null), Instant.now(), false);

    @Test
    void ciclodeVidaAteODesfecho() {
        assertThat(request.status()).isEqualTo(RequestStatus.IN_PROGRESS);
        request.attachProcess("pi-1", "purchaseApproval:1:x");
        request.finish(RequestStatus.APPROVED, Instant.now());
        assertThat(request.status()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.finishedAt()).isNotNull();
    }

    @Test
    void naoPermiteEncerrarDuasVezesNemDesfechoInvalido() {
        assertThatThrownBy(() -> request.finish(RequestStatus.IN_PROGRESS, Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
        request.finish(RequestStatus.REJECTED, Instant.now());
        assertThatThrownBy(() -> request.finish(RequestStatus.APPROVED, Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void falhaNaoSobrescreveDesfechoFinal() {
        request.markFailed("erro");
        assertThat(request.status()).isEqualTo(RequestStatus.FAILED);
        request.finish(RequestStatus.APPROVED, Instant.now());
        request.markFailed("outro erro");
        assertThat(request.status()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void processoSoPodeSerAssociadoUmaVez() {
        request.attachProcess("pi-1", null);
        assertThatThrownBy(() -> request.attachProcess("pi-2", null)).isInstanceOf(BusinessRuleViolationException.class);
    }
}
