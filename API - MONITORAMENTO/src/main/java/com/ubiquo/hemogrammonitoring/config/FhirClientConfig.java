package com.ubiquo.hemogrammonitoring.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirClientConfig {
    
    private final FhirProperties fhirProperties;
    
    public FhirClientConfig(FhirProperties fhirProperties) {
        this.fhirProperties = fhirProperties;
    }
    
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
    
    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return fhirContext.newRestfulGenericClient(fhirProperties.getServerUrl());
    }
}

