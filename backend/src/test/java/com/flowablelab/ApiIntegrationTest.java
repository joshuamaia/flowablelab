package com.flowablelab;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contratos REST e tratamento de erros (Problem Details) sobre o motor real. */
class ApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    private static final String PURCHASE = """
            {"processKey":"purchaseApproval","requester":"Ana API","description":"Teclados","amount":350.00}
            """;

    private String startPurchase() throws Exception {
        String body = mvc.perform(post("/api/process-instances").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Demo-User", "colaborador").content(PURCHASE))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.request.number").value(containsString("REQ-")))
                .andExpect(jsonPath("$.request.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.activeTasks[0].taskDefinitionKey").value("managerReview"))
                .andExpect(jsonPath("$.activeTasks[0].allowedActions", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.activeTasks[0].id");
    }

    private static String complete(String action, String comment) {
        return comment == null ? "{\"action\":\"%s\"}".formatted(action)
                : "{\"action\":\"%s\",\"comment\":\"%s\"}".formatted(action, comment);
    }

    @Test
    void cicloCompletoDeUmaTarefaComValidacoes() throws Exception {
        String taskId = startPurchase();
        String url = "/api/tasks/" + taskId + "/complete";

        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).header("X-Demo-User", "ti")
                        .content(complete("APPROVE", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(containsString("user-not-allowed")));

        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).header("X-Demo-User", "gestor")
                        .content(complete("REJECT", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("justificativa")));

        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).header("X-Demo-User", "gestor")
                        .content(complete("APPROVE", "ok")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processEnded").value(true));

        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).header("X-Demo-User", "gestor")
                        .content(complete("APPROVE", "de novo")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Tarefa já concluída"));
    }

    @Test
    void processoInexistenteEDadosInvalidos() throws Exception {
        mvc.perform(post("/api/process-instances").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processKey\":\"naoExiste\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Processo inexistente"));

        mvc.perform(post("/api/process-instances").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processKey\":\"purchaseApproval\",\"requester\":\"\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItems("requester", "amount",
                        "description")))
                .andExpect(content().string(not(containsString("Exception"))));

        mvc.perform(post("/api/process-instances").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processKey":"vacationApproval","requester":"Bia","startDate":"2031-01-10","endDate":"2031-01-05"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("anterior")));
    }

    @Test
    void recursosInexistentesRetornam404() throws Exception {
        mvc.perform(get("/api/process-instances/nao-existe")).andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks/nao-existe")).andExpect(status().isNotFound());
        mvc.perform(get("/api/process-definitions/x:1:y")).andExpect(status().isNotFound());
        mvc.perform(post("/api/tasks/nao-existe/complete").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Demo-User", "gestor").content(complete("APPROVE", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void definicoesDiagramaHistoricoEDashboard() throws Exception {
        String definitions = mvc.perform(get("/api/process-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andReturn().getResponse().getContentAsString();
        String definitionId = JsonPath.read(definitions, "$[0].id");
        mvc.perform(get("/api/process-definitions/" + definitionId + "/diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bpmndi:BPMNPlane")));

        String taskId = startPurchase();
        String task = mvc.perform(get("/api/tasks/" + taskId)).andReturn().getResponse().getContentAsString();
        String pid = JsonPath.read(task, "$.processInstanceId");

        mvc.perform(get("/api/tasks").param("assignee", "gestor").param("processKey", "purchaseApproval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItem(taskId)));
        mvc.perform(get("/api/process-instances/" + pid + "/activities"))
                .andExpect(jsonPath("$.elements[?(@.elementId=='managerReview')].status").value("ACTIVE"));
        mvc.perform(get("/api/process-instances/" + pid + "/variables"))
                .andExpect(jsonPath("$[?(@.name=='requestNumber')]").exists());
        mvc.perform(get("/api/history/processes/" + pid + "/activities"))
                .andExpect(jsonPath("$[0].type").value("startEvent"));
        mvc.perform(get("/api/history/processes/" + pid + "/tasks")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/requests").param("status", "IN_PROGRESS").param("type", "PURCHASE"))
                .andExpect(jsonPath("$.content[0].currentSteps[0]").value("Análise do gestor"));
        mvc.perform(get("/api/dashboard/summary")).andExpect(jsonPath("$.pendingTasks").isNumber());
        mvc.perform(get("/api/dashboard/process-status")).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/dashboard/process-types")).andExpect(jsonPath("$", hasSize(4)));
    }
}
