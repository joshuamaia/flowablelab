package com.flowablelab.application.model;

import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.RequestType;
import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.domain.workflow.TaskStatus;

/** Filtros de consulta utilizados pelos casos de uso. Campos nulos são ignorados. */
public final class Filters {

    private Filters() {
    }

    public record RequestFilter(RequestStatus status, RequestType type, String requester) {
    }

    public record ProcessInstanceFilter(String processDefinitionKey, Boolean finished, String businessKey) {
    }

    /**
     * @param user           tarefas elegíveis (pendentes) ou concluídas por este usuário
     * @param processDefinitionKey filtra por processo
     * @param status         PENDING (padrão) ou COMPLETED
     */
    public record TaskFilter(DemoUser user, String processDefinitionKey, TaskStatus status) {

        public TaskFilter {
            status = status == null ? TaskStatus.PENDING : status;
        }
    }
}
