package com.ubiquo.hemogrammonitoring.service;

import com.ubiquo.hemogrammonitoring.model.HemogramData;
import com.ubiquo.hemogrammonitoring.model.ReferenceValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class FhirParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(FhirParserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Processa um JSON FHIR e extrai dados do hemograma
     */
    public HemogramData parseFhirObservation(String fhirJson) {
        try {
            JsonNode root = objectMapper.readTree(fhirJson);
            
            // Extrair ID da observação
            String observationId = root.path("id").asText();
            
            // Extrair ID do paciente
            String patientId = extractPatientId(root);
            
            // Extrair timestamp
            LocalDateTime timestamp = extractTimestamp(root);
            
            // Extrair região (mockado por enquanto)
            String region = "Goiânia"; // TODO: Extrair da localização do paciente
            
            // Extrair valores dos parâmetros hematológicos
            Double leucocitos = extractParameterValue(root, ReferenceValues.LEUCOCITOS_LOINC);
            Double hemoglobina = extractParameterValue(root, ReferenceValues.HEMOGLOBINA_LOINC);
            Double plaquetas = extractParameterValue(root, ReferenceValues.PLAQUETAS_LOINC);
            Double hematocrito = extractParameterValue(root, ReferenceValues.HEMATOCRITO_LOINC);
            
            HemogramData hemogramData = new HemogramData(
                observationId, 
                patientId, 
                timestamp, 
                leucocitos, 
                hemoglobina, 
                plaquetas, 
                hematocrito, 
                region
            );
            
            logger.info("Dados do hemograma extraídos: {}", hemogramData);
            return hemogramData;
            
        } catch (Exception e) {
            logger.error("Erro ao processar JSON FHIR: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extrai o ID do paciente do recurso FHIR
     */
    private String extractPatientId(JsonNode root) {
        try {
            return root.path("subject").path("reference").asText().replace("Patient/", "");
        } catch (Exception e) {
            logger.warn("Não foi possível extrair ID do paciente: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Extrai o timestamp da observação
     */
    private LocalDateTime extractTimestamp(JsonNode root) {
        try {
            String dateString = root.path("effectiveDateTime").asText();
            if (dateString.isEmpty()) {
                dateString = root.path("issued").asText();
            }
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            logger.warn("Não foi possível extrair timestamp, usando data atual: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }
    
    /**
     * Extrai o valor de um parâmetro específico usando código LOINC
     */
    private Double extractParameterValue(JsonNode root, String loincCode) {
        try {
            JsonNode component = root.path("component");
            if (component.isArray()) {
                for (JsonNode comp : component) {
                    JsonNode code = comp.path("code");
                    if (code.path("coding").isArray()) {
                        for (JsonNode coding : code.path("coding")) {
                            if (loincCode.equals(coding.path("code").asText())) {
                                JsonNode valueQuantity = comp.path("valueQuantity");
                                if (!valueQuantity.isMissingNode()) {
                                    return valueQuantity.path("value").asDouble();
                                }
                            }
                        }
                    }
                }
            }
            logger.warn("Parâmetro com código LOINC {} não encontrado", loincCode);
            return null;
        } catch (Exception e) {
            logger.warn("Erro ao extrair parâmetro com código LOINC {}: {}", loincCode, e.getMessage());
            return null;
        }
    }
    
    /**
     * Analisa o hemograma e identifica desvios nos parâmetros
     */
    public List<String> analyzeHemogram(HemogramData hemogram) {
        List<String> deviations = new ArrayList<>();
        
        if (hemogram.getLeucocitos() != null) {
            if (!ReferenceValues.isLeucocitosNormal(hemogram.getLeucocitos())) {
                deviations.add(String.format("Leucócitos alterados: %.2f /µL (normal: %.0f-%.0f)", 
                    hemogram.getLeucocitos(), ReferenceValues.LEUCOCITOS_MIN, ReferenceValues.LEUCOCITOS_MAX));
            }
        }
        
        if (hemogram.getHemoglobina() != null) {
            if (!ReferenceValues.isHemoglobinaNormal(hemogram.getHemoglobina())) {
                deviations.add(String.format("Hemoglobina alterada: %.2f g/dL (normal: %.1f-%.1f)", 
                    hemogram.getHemoglobina(), ReferenceValues.HEMOGLOBINA_MIN, ReferenceValues.HEMOGLOBINA_MAX));
            }
        }
        
        if (hemogram.getPlaquetas() != null) {
            if (!ReferenceValues.isPlaquetasNormal(hemogram.getPlaquetas())) {
                deviations.add(String.format("Plaquetas alteradas: %.0f /µL (normal: %.0f-%.0f)", 
                    hemogram.getPlaquetas(), ReferenceValues.PLAQUETAS_MIN, ReferenceValues.PLAQUETAS_MAX));
                
                // Verificar se as plaquetas estão baixas (indicativo de dengue)
                if (ReferenceValues.isPlaquetasBaixas(hemogram.getPlaquetas())) {
                    deviations.add("⚠️ ALERTA DENGUE: Plaquetas baixas detectadas!");
                }
            }
        }
        
        if (hemogram.getHematocrito() != null) {
            if (!ReferenceValues.isHematocritoNormal(hemogram.getHematocrito())) {
                deviations.add(String.format("Hematócrito alterado: %.2f%% (normal: %.0f-%.0f%%)", 
                    hemogram.getHematocrito(), ReferenceValues.HEMATOCRITO_MIN, ReferenceValues.HEMATOCRITO_MAX));
            }
        }
        
        return deviations;
    }
}
