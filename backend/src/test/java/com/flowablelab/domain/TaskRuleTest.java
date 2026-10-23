package com.flowablelab.domain;

import com.flowablelab.domain.exception.InvalidTaskActionException;
import com.flowablelab.domain.task.TaskAction;
import com.flowablelab.domain.task.TaskRules;
import com.flowablelab.domain.user.DemoUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskRuleTest {

    @Test
    void aprovacaoDoGestorGravaVariavelDeDecisao() {
        var variables = TaskRules.forTask("managerReview").resolve(TaskAction.APPROVE, " ok ");
        assertThat(variables).containsEntry("approved", true).containsEntry("managerReviewComment", "ok");
    }

    @Test
    void rejeicaoExigeJustificativa() {
        assertThatThrownBy(() -> TaskRules.forTask("managerReview").resolve(TaskAction.REJECT, "  "))
                .isInstanceOf(InvalidTaskActionException.class)
                .hasMessageContaining("justificativa");
        assertThat(TaskRules.forTask("financialAnalysis").resolve(TaskAction.REJECT, "sem verba"))
                .containsEntry("financialApproved", false);
    }

    @Test
    void acaoPrecisaSerPermitidaParaATarefa() {
        assertThatThrownBy(() -> TaskRules.forTask("managerReview").resolve(TaskAction.COMPLETE, null))
                .isInstanceOf(InvalidTaskActionException.class);
        assertThatThrownBy(() -> TaskRules.forTask("itProvisioning").resolve(TaskAction.APPROVE, null))
                .isInstanceOf(InvalidTaskActionException.class);
        assertThat(TaskRules.forTask("itProvisioning").resolve(TaskAction.COMPLETE, null)).isEmpty();
    }

    @Test
    void elegibilidadePorGrupoOuResponsavel() {
        assertThat(DemoUser.GESTOR.canWorkOn(null, List.of("gestor"))).isTrue();
        assertThat(DemoUser.TI.canWorkOn(null, List.of("gestor"))).isFalse();
        assertThat(DemoUser.TI.canWorkOn("ti", List.of("gestor"))).isTrue();
        assertThat(DemoUser.GESTOR.canWorkOn("ti", List.of("gestor"))).isFalse();
    }

    @Test
    void perfilDesconhecidoNaoEAceito() {
        assertThatThrownBy(() -> DemoUser.fromId("admin")).hasMessageContaining("desconhecido");
        assertThat(DemoUser.fromId(" GESTOR ")).isEqualTo(DemoUser.GESTOR);
    }
}
