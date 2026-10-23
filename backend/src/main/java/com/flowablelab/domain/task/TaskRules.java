package com.flowablelab.domain.task;

import java.util.List;
import java.util.Map;

import static com.flowablelab.domain.task.TaskAction.APPROVE;
import static com.flowablelab.domain.task.TaskAction.COMPLETE;
import static com.flowablelab.domain.task.TaskAction.REJECT;

/** Catálogo das regras de conclusão das User Tasks definidas nos arquivos BPMN. */
public final class TaskRules {

    private static final Map<String, TaskRule> RULES = Map.of(
            "managerReview", new TaskRule("managerReview", List.of(APPROVE, REJECT), "approved"),
            "itProvisioning", new TaskRule("itProvisioning", List.of(COMPLETE), null),
            "openRequest", new TaskRule("openRequest", List.of(COMPLETE), null),
            "technicalAnalysis", new TaskRule("technicalAnalysis", List.of(APPROVE, REJECT), "technicalApproved"),
            "financialAnalysis", new TaskRule("financialAnalysis", List.of(APPROVE, REJECT), "financialApproved"));

    private TaskRules() {
    }

    /** Tarefas não catalogadas aceitam apenas a conclusão simples. */
    public static TaskRule forTask(String taskDefinitionKey) {
        return RULES.getOrDefault(taskDefinitionKey, new TaskRule(taskDefinitionKey, List.of(COMPLETE), null));
    }
}
