package com.flowablelab.domain.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolida o histórico real de execução em estados por elemento do diagrama.
 *
 * <p>Regras: um elemento só é marcado quando realmente aparece no histórico (nada é
 * inferido a partir da ordem do fluxo). Falha tem precedência sobre ativo, e ativo tem
 * precedência sobre concluído — assim um elemento executado em loop que está novamente
 * em andamento nunca aparece como concluído.</p>
 */
public final class ExecutionPath {

    private ExecutionPath() {
    }

    /**
     * @param executions        histórico de execução da instância
     * @param failures          falhas registradas pelo motor
     * @param waitingElementIds elementos onde há execuções paradas no runtime (ex.: um Parallel
     *                          Gateway de junção aguardando os demais ramos, cujo registro histórico
     *                          do ramo que já chegou aparece encerrado)
     */
    public static List<ElementState> summarize(List<ActivityExecution> executions,
                                               Collection<ElementFailure> failures,
                                               Collection<String> waitingElementIds) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Boolean> active = new LinkedHashMap<>();
        for (ActivityExecution execution : executions) {
            counts.merge(execution.activityId(), 1, Integer::sum);
            active.merge(execution.activityId(), execution.isActive(), Boolean::logicalOr);
        }
        waitingElementIds.forEach(elementId -> {
            if (counts.containsKey(elementId)) {
                active.put(elementId, true);
            }
        });

        Map<String, ElementFailure> failuresByElement = new LinkedHashMap<>();
        failures.forEach(failure -> failuresByElement.putIfAbsent(failure.elementId(), failure));

        List<ElementState> states = new ArrayList<>();
        counts.forEach((elementId, count) -> {
            ElementFailure failure = failuresByElement.remove(elementId);
            if (failure != null) {
                states.add(new ElementState(elementId, ActivityStatus.FAILED, count, failure.message()));
            } else {
                ActivityStatus status = active.get(elementId) ? ActivityStatus.ACTIVE : ActivityStatus.COMPLETED;
                states.add(new ElementState(elementId, status, count, null));
            }
        });
        // Elementos assíncronos que falharam antes de gerar histórico de atividade.
        failuresByElement.values().forEach(failure ->
                states.add(new ElementState(failure.elementId(), ActivityStatus.FAILED, 0, failure.message())));
        return states;
    }
}
