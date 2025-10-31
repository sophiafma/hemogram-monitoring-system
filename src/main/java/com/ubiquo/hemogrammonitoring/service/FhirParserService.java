package com.ubiquo.hemogrammonitoring.service;

import com.ubiquo.hemogrammonitoring.entity.HemogramEntity;
import com.ubiquo.hemogrammonitoring.model.HemogramData;
import com.ubiquo.hemogrammonitoring.model.ReferenceValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubiquo.hemogrammonitoring.repository.HemogramRepository;
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
    private final HemogramRepository hemogramRepository;

    public FhirParserService(HemogramRepository hemogramRepository) {
        this.hemogramRepository = hemogramRepository;
    }

    /**
     * Processa um JSON FHIR, extrai dados do hemograma e o salva no banco de dados.
     */
    public HemogramData parseFhirObservation(String fhirJson) {
        try {
            JsonNode root = objectMapper.readTree(fhirJson);

            String observationId = root.path("id").asText("N/A");

            // Extrair dados do paciente contido no JSON
            JsonNode containedPatient = findContainedPatient(root);
            String patientId = extractPatientId(root);
            String patientName = extractContainedPatientName(containedPatient);
            String patientCpf = extractContainedPatientCpf(containedPatient);
            String patientPhone = extractContainedPatientPhone(containedPatient);

            logger.info("Processando hemograma para o paciente: {} (CPF: {}, Tel: {})", patientName, patientCpf, patientPhone);

            LocalDateTime timestamp = extractTimestamp(root);
            String region = "Goiânia";

            Double leucocitos = extractParameterValue(root, ReferenceValues.LEUCOCITOS_LOINC);
            Double hemoglobina = extractParameterValue(root, ReferenceValues.HEMOGLOBINA_LOINC);
            Double plaquetas = extractParameterValue(root, ReferenceValues.PLAQUETAS_LOINC);
            Double hematocrito = extractParameterValue(root, ReferenceValues.HEMATOCRITO_LOINC);

            HemogramData hemogramData = new HemogramData(
                    observationId, patientId, patientName, patientCpf, patientPhone,
                    timestamp, leucocitos, hemoglobina, plaquetas, hematocrito, region
            );

            logger.info("Dados do hemograma extraídos: {}", hemogramData);

            // Salvar no banco de dados
            saveHemogram(hemogramData);

            return hemogramData;

        } catch (Exception e) {
            logger.error("Erro ao processar JSON FHIR: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Salva os dados do hemograma no banco de dados.
     */
    private void saveHemogram(HemogramData data) {
        if (data == null) {
            logger.warn("Tentativa de salvar um hemograma nulo.");
            return;
        }
        try {
            HemogramEntity entity = new HemogramEntity();
            entity.setObservationId(data.getId());
            entity.setPatientId(data.getPatientId());
            entity.setPatientName(data.getPatientName());
            entity.setPatientCpf(data.getPatientCpf());
            entity.setPatientPhone(data.getPatientPhone());
            entity.setTimestamp(data.getTimestamp());
            entity.setLeucocitos(data.getLeucocitos());
            entity.setHemoglobina(data.getHemoglobina());
            entity.setPlaquetas(data.getPlaquetas());
            entity.setHematocrito(data.getHematocrito());
            entity.setRegion(data.getRegion());

            hemogramRepository.save(entity);
            logger.info("✅ Hemograma para o paciente '{}' (CPF: {}) salvo no banco de dados.", data.getPatientName(), data.getPatientCpf());
        } catch (Exception e) {
            logger.error("❌ Erro ao salvar hemograma no banco de dados para o paciente {}: {}", data.getPatientName(), e.getMessage(), e);
        }
    }

    private JsonNode findContainedPatient(JsonNode root) {
        JsonNode contained = root.path("contained");
        if (contained.isArray()) {
            for (JsonNode resource : contained) {
                if ("Patient".equals(resource.path("resourceType").asText())) {
                    return resource;
                }
            }
        }
        return null;
    }

    private String extractContainedPatientName(JsonNode patientNode) {
        if (patientNode == null) return "Nome não encontrado";
        return patientNode.path("name").get(0).path("text").asText("Nome não encontrado");
    }

    private String extractContainedPatientCpf(JsonNode patientNode) {
        if (patientNode == null) return "CPF não encontrado";
        JsonNode identifier = patientNode.path("identifier");
        if (identifier.isArray()) {
            for (JsonNode id : identifier) {
                if ("urn:oid:2.16.840.1.113883.4.642.3.1".equals(id.path("system").asText())) {
                    return id.path("value").asText("CPF não encontrado");
                }
            }
        }
        return "CPF não encontrado";
    }

    private String extractContainedPatientPhone(JsonNode patientNode) {
        if (patientNode == null) return "Telefone não encontrado";
        JsonNode telecom = patientNode.path("telecom");
        if (telecom.isArray()) {
            for (JsonNode contact : telecom) {
                if ("phone".equals(contact.path("system").asText())) {
                    return contact.path("value").asText("Telefone não encontrado");
                }
            }
        }
        return "Telefone não encontrado";
    }

    private String extractPatientId(JsonNode root) {
        try {
            String reference = root.path("subject").path("reference").asText();
            if (reference.startsWith("#")) {
                return reference.substring(1);
            }
            return reference.replace("Patient/", "");
        } catch (Exception e) {
            logger.warn("Não foi possível extrair ID do paciente: {}", e.getMessage());
            return "unknown";
        }
    }

    private LocalDateTime extractTimestamp(JsonNode root) {
        try {
            String dateString = root.path("effectiveDateTime").asText();
            if (dateString.isEmpty()) {
                dateString = root.path("issued").asText();
            }
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            logger.warn("Não foi possível extrair timestamp, usando data atual: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    private Double extractParameterValue(JsonNode root, String loincCode) {
        try {
            JsonNode codings = root.path("code").path("coding");
            if (codings.isArray()) {
                for (JsonNode coding : codings) {
                    if (loincCode.equals(coding.path("code").asText())) {
                        JsonNode valueQuantity = root.path("valueQuantity");
                        if (!valueQuantity.isMissingNode()) {
                            return valueQuantity.path("value").asDouble();
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
