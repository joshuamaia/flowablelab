package com.flowablelab.infrastructure.flowable;

import com.flowablelab.application.port.out.ProcessEnginePort;
import com.flowablelab.domain.request.RequestType;
import com.flowablelab.domain.workflow.ProcessDefinitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Após a implantação automática feita pelo Flowable, confirma que todas as definições
 * exigidas pela aplicação existem. Se alguma faltar, a inicialização é interrompida.
 */
@Component
@Order(1)
public class ProcessDeploymentVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessDeploymentVerifier.class);

    private final ProcessEnginePort engine;

    public ProcessDeploymentVerifier(ProcessEnginePort engine) {
        this.engine = engine;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = Arrays.stream(RequestType.values())
                .map(RequestType::processKey)
                .filter(key -> engine.findLatestDefinitionByKey(key).isEmpty())
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Definições BPMN não implantadas: " + missing);
        }
        for (ProcessDefinitionInfo definition : engine.latestDefinitions()) {
            log.info("Definição BPMN disponível: {} v{} ({})", definition.key(), definition.version(),
                    definition.id());
        }
    }
}
