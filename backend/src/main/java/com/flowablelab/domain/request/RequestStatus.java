package com.flowablelab.domain.request;

public enum RequestStatus {
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    FAILED;

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED;
    }
}
