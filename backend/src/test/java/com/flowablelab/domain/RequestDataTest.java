package com.flowablelab.domain;

import com.flowablelab.domain.exception.BusinessRuleViolationException;
import com.flowablelab.domain.request.AccessData;
import com.flowablelab.domain.request.AccessLevel;
import com.flowablelab.domain.request.ParallelAnalysisData;
import com.flowablelab.domain.request.PurchaseData;
import com.flowablelab.domain.request.VacationData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestDataTest {

    @Nested
    class Purchase {

        @Test
        void exigeValorMaiorQueZero() {
            assertThatThrownBy(() -> new PurchaseData("Ana", "Notebook", BigDecimal.ZERO, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("maior que zero");
        }

        @Test
        void exigeDescricao() {
            assertThatThrownBy(() -> new PurchaseData("Ana", "  ", BigDecimal.TEN, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("descrição");
        }

        @Test
        void classificaPrioridadePeloValor() {
            assertThat(new PurchaseData("Ana", "Mouse", new BigDecimal("99.90"), null).priority()).isEqualTo("NORMAL");
            assertThat(new PurchaseData("Ana", "Servidor", new BigDecimal("10000"), null).priority()).isEqualTo("ALTA");
        }

        @Test
        void geraVariaveisSimplesParaOProcesso() {
            var variables = new PurchaseData(" Ana ", "Notebook", new BigDecimal("1500.50"), null).toProcessVariables();
            assertThat(variables).containsEntry("requester", "Ana").containsEntry("amount", 1500.5)
                    .containsEntry("justification", "");
        }
    }

    @Nested
    class Vacation {

        private final LocalDate start = LocalDate.of(2030, 1, 10);

        @Test
        void rejeitaDataFinalAnteriorAInicial() {
            assertThatThrownBy(() -> new VacationData("Bruno", start, start.minusDays(1), null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("anterior");
        }

        @Test
        void validaDuracaoDoPeriodo() {
            assertThatThrownBy(() -> new VacationData("Bruno", start, start.plusDays(2), null))
                    .hasMessageContaining("entre 5 e 30 dias");
            assertThatThrownBy(() -> new VacationData("Bruno", start, start.plusDays(30), null))
                    .hasMessageContaining("entre 5 e 30 dias");
            assertThat(new VacationData("Bruno", start, start.plusDays(4), null).days()).isEqualTo(5);
        }

        @Test
        void naoAceitaInicioNoPassado() {
            var data = new VacationData("Bruno", start, start.plusDays(9), null);
            assertThatThrownBy(() -> data.requireStartNotBefore(start.plusDays(1)))
                    .hasMessageContaining("passado");
        }
    }

    @Test
    void acessoExigeJustificativaENivel() {
        assertThatThrownBy(() -> new AccessData("Carla", "ERP", AccessLevel.LEITURA, ""))
                .hasMessageContaining("justificativa");
        assertThatThrownBy(() -> new AccessData("Carla", "ERP", null, "fechamento"))
                .hasMessageContaining("nível de acesso");
    }

    @Test
    void paraleloUsaValorEstimadoComoValor() {
        var data = new ParallelAnalysisData("Diego", "Nuvem", "Migrar", new BigDecimal("500"));
        assertThat(data.amount()).isEqualByComparingTo("500");
        assertThat(data.summary()).isEqualTo("Nuvem");
    }
}
