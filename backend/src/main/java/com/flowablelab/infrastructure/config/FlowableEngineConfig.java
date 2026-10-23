package com.flowablelab.infrastructure.config;

import com.flowablelab.application.usecase.RequestLifecycleUseCase;
import com.flowablelab.infrastructure.flowable.JobFailureListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Ajustes do Process Engine incorporado. A configuração principal (DataSource, transações,
 * implantação automática de /processes) é feita pelo starter oficial do Flowable.
 */
@Configuration
public class FlowableEngineConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> flowableLabEngineConfigurer(@Lazy RequestLifecycleUseCase lifecycle) {
        return configuration -> configuration.setTypedEventListeners(Map.of(
                FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER.name(), List.of(new JobFailureListener(lifecycle))));
    }
}
