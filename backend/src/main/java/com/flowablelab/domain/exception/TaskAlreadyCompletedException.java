package com.flowablelab.domain.exception;

public class TaskAlreadyCompletedException extends DomainException {

    public TaskAlreadyCompletedException(String taskId) {
        super("A tarefa '%s' já foi concluída.".formatted(taskId));
    }
}
