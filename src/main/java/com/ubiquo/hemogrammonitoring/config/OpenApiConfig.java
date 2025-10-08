package com.ubiquo.hemogrammonitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
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
                        .version("1.0.0 - Marco 1")
                        .description("""
                                ## 🏥 Sistema de Monitoramento de Hemogramas
                                
                                Sistema desenvolvido para a disciplina de **Software para Sistemas Ubíquos** que monitora 
                                hemogramas em tempo real para detectar possíveis surtos de dengue baseados em plaquetas baixas.
                                
                                ### 📊 Funcionalidades (Marco 1)
                                - ✅ Recepção de dados FHIR via subscription
                                - ✅ Processamento de hemogramas usando códigos LOINC
                                - ✅ Detecção automática de alertas de dengue
                                - ✅ Análise de desvios nos parâmetros hematológicos
                                
                                ### 🔬 Parâmetros Monitorados
                                | Parâmetro | Código LOINC | Valor Normal |
                                |-----------|--------------|--------------|
                                | Leucócitos | 33747-0 | 4.000-11.000 /µL |
                                | Hemoglobina | 718-7 | 12.0-17.5 g/dL |
                                | Plaquetas | 777-3 | 150.000-450.000 /µL |
                                | Hematócrito | 4544-3 | 36-52% |
                                
                                ### 🚨 Detecção de Dengue
                                O sistema emite alerta automático quando detecta plaquetas abaixo de 150.000 /µL,
                                que é um dos principais indicadores de dengue.
                                """)
                        .contact(new Contact()
                                .name("Equipe de Desenvolvimento")
                                .email("contato@hemogram-monitoring.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Servidor de Desenvolvimento")
                ));
    }
}

