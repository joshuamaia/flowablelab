package com.flowablelab.domain;

import com.flowablelab.domain.workflow.ActivityExecution;
import com.flowablelab.domain.workflow.ActivityStatus;
import com.flowablelab.domain.workflow.ElementFailure;
import com.flowablelab.domain.workflow.ElementState;
import com.flowablelab.domain.workflow.ExecutionPath;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionPathTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private static ActivityExecution done(String id) {
        return new ActivityExecution(id + "-1", id, id, "userTask", "e", null, null, T0, T0.plusSeconds(1), 1000L,
                null, null);
    }

    private static ActivityExecution running(String id) {
        return new ActivityExecution(id + "-2", id, id, "userTask", "e", null, null, T0, null, null, null, null);
    }

    private static Map<String, ElementState> byId(List<ElementState> states) {
        return states.stream().collect(Collectors.toMap(ElementState::elementId, s -> s));
    }

    @Test
    void marcaSomenteElementosPresentesNoHistorico() {
        var states = byId(ExecutionPath.summarize(List.of(done("start"), running("review")), List.of(), List.of()));
        assertThat(states).containsOnlyKeys("start", "review");
        assertThat(states.get("start").status()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(states.get("review").status()).isEqualTo(ActivityStatus.ACTIVE);
    }

    @Test
    void elementoEmLoopNovamenteAtivoNaoApareceConcluido() {
        var states = byId(ExecutionPath.summarize(List.of(done("review"), running("review")), List.of(), List.of()));
        assertThat(states.get("review").status()).isEqualTo(ActivityStatus.ACTIVE);
        assertThat(states.get("review").executionCount()).isEqualTo(2);
    }

    @Test
    void juncaoParalelaAguardandoRamosEhAtiva() {
        var states = byId(ExecutionPath.summarize(List.of(done("technical"), done("join"), running("financial")),
                List.of(), List.of("join", "financial")));
        assertThat(states.get("join").status()).isEqualTo(ActivityStatus.ACTIVE);
        assertThat(states.get("technical").status()).isEqualTo(ActivityStatus.COMPLETED);
    }

    @Test
    void falhaTemPrecedenciaEIncluiElementoSemHistorico() {
        var states = byId(ExecutionPath.summarize(List.of(running("hr")),
                List.of(new ElementFailure("hr", "conflito"), new ElementFailure("asyncOnly", "erro")), List.of()));
        assertThat(states.get("hr").status()).isEqualTo(ActivityStatus.FAILED);
        assertThat(states.get("hr").failureMessage()).isEqualTo("conflito");
        assertThat(states.get("asyncOnly").status()).isEqualTo(ActivityStatus.FAILED);
        assertThat(states.get("asyncOnly").executionCount()).isZero();
    }
}
