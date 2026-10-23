package com.flowablelab.domain.workflow;

/**
 * Estado consolidado de um elemento do diagrama BPMN para fins de visualização.
 *
 * @param elementId      id do elemento no XML BPMN
 * @param status         ACTIVE, COMPLETED ou FAILED
 * @param executionCount quantas vezes o elemento foi executado
 * @param failureMessage mensagem do job com falha, quando houver
 */
public record ElementState(String elementId, ActivityStatus status, int executionCount, String failureMessage) {
}
