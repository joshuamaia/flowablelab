package com.flowablelab.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flowableLabOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FlowableLab API")
                .version("1.0.0")
                .description("""
                        API REST do FlowableLab: execução de workflows BPMN 2.0 com o Flowable Open Source \
                        incorporado ao Spring Boot.

                        **Ambiente demonstrativo:** o cabeçalho `X-Demo-User` apenas simula perfis \
                        (colaborador, gestor, ti) e NÃO é autenticação. Não exponha esta API publicamente.""")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                .contact(new Contact().name("FlowableLab")));
    }
}
