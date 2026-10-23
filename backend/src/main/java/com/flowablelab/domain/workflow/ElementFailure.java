package com.flowablelab.domain.workflow;

/** Falha registrada pelo motor para um elemento (job com exceção ou na fila de dead letter). */
public record ElementFailure(String elementId, String message) {
}
