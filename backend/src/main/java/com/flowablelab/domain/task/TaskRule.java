package com.flowablelab.domain.task;

import com.flowablelab.domain.exception.InvalidTaskActionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regra de conclusão de uma User Task: quais ações são permitidas e em qual
 * variável de processo a decisão é gravada para ser avaliada pelos gateways.
 *
 * @param taskDefinitionKey id da User Task no BPMN
 * @param allowedActions    ações aceitas
 * @param decisionVariable  variável booleana de decisão (null quando a tarefa não decide nada)
 */
public record TaskRule(String taskDefinitionKey, List<TaskAction> allowedActions, String decisionVariable) {

    public static final int MAX_COMMENT_LENGTH = 1000;

    public TaskRule {
        allowedActions = List.copyOf(allowedActions);
    }

    public boolean isDecision() {
        return decisionVariable != null;
    }

    /**
     * Valida a ação solicitada e devolve as variáveis de processo que devem ser gravadas.
     * Rejeições exigem justificativa.
     */
    public Map<String, Object> resolve(TaskAction action, String comment) {
        if (action == null || !allowedActions.contains(action)) {
            throw new InvalidTaskActionException("A ação %s não é permitida para a tarefa '%s'. Ações aceitas: %s."
                    .formatted(action, taskDefinitionKey, allowedActions));
        }
        String normalizedComment = comment == null ? "" : comment.trim();
        if (normalizedComment.length() > MAX_COMMENT_LENGTH) {
            throw new InvalidTaskActionException(
                    "O comentário deve ter no máximo %d caracteres.".formatted(MAX_COMMENT_LENGTH));
        }
        if (action == TaskAction.REJECT && normalizedComment.isEmpty()) {
            throw new InvalidTaskActionException("A rejeição exige uma justificativa.");
        }
        Map<String, Object> variables = new HashMap<>();
        if (isDecision()) {
            variables.put(decisionVariable, action == TaskAction.APPROVE);
            variables.put(taskDefinitionKey + "Comment", normalizedComment);
        }
        return variables;
    }
}
