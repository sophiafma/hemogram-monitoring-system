package com.ubiquo.hemogrammonitoring.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do HAPI FHIR Context.
 * 
 * FhirContext é o ponto de entrada principal do HAPI FHIR.
 * Ele fornece parsers, validators e outras funcionalidades.
 * É thread-safe e deve ser singleton (reutilizado).
 */
@Configuration
public class FhirContextConfig {
    
    /**
     * Cria instância do FhirContext para FHIR R4.
     * 
     * @return FhirContext configurado para trabalhar com recursos FHIR R4
     */
    @Bean
    public FhirContext fhirContext() {
        // Cria contexto para FHIR R4 (versão usada pela SES-GO)
        return FhirContext.forR4();
    }
}

