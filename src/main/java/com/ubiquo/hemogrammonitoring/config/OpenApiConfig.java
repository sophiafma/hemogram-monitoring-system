package com.ubiquo.hemogrammonitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Monitoramento de Hemogramas")
                        .version("1.0.0")
                        .description("""
                                ## 🏥 Sistema de Monitoramento de Hemogramas
                                
                                Plataforma para monitorar hemogramas em tempo real, detectar desvios nos
                                parâmetros hematológicos e emitir alertas para possíveis casos de dengue.
                                
                                ### 🔬 Parâmetros Monitorados
                                | Parâmetro | Código LOINC | Valor Normal |
                                |-----------|--------------|--------------|
                                | Leucócitos | 33747-0 | 4.000-11.000 /µL |
                                | Hemoglobina | 718-7 | 12.0-17.5 g/dL |
                                | Plaquetas | 777-3 | 150.000-450.000 /µL |
                                | Hematócrito | 4544-3 | 36-52% |
                                
                                ### 🚨 Detecção de Dengue
                                O sistema emite alerta automático quando detecta plaquetas abaixo de 150.000 /µL.
                                """))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Servidor de Desenvolvimento")
                ));
    }
}

