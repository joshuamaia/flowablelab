package com.flowablelab.domain.request;

import java.util.Arrays;
import java.util.Optional;

/** Tipos de solicitação de negócio e a definição BPMN (processKey) que executa cada um. */
public enum RequestType {

    PURCHASE("purchaseApproval", "Aprovação de compra"),
    VACATION("vacationApproval", "Aprovação de férias"),
    ACCESS("accessRequest", "Solicitação de acesso"),
    PARALLEL("parallelApproval", "Aprovação paralela");

    private final String processKey;
    private final String label;

    RequestType(String processKey, String label) {
        this.processKey = processKey;
        this.label = label;
    }

    public String processKey() {
        return processKey;
    }

    public String label() {
        return label;
    }

    public static Optional<RequestType> fromProcessKey(String processKey) {
        return Arrays.stream(values()).filter(type -> type.processKey.equals(processKey)).findFirst();
    }
}
