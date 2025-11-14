package com.ubiquo.hemogrammonitoring.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.ubiquo.hemogrammonitoring.entity.HemogramEntity;
import com.ubiquo.hemogrammonitoring.model.HemogramData;
import com.ubiquo.hemogrammonitoring.model.ReferenceValues;
import com.ubiquo.hemogrammonitoring.repository.HemogramRepository;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service refatorado para usar HAPI FHIR corretamente.
 * 
 * Melhorias:
 * - Usa classes HAPI (Bundle, Observation) em vez de parsing manual
 * - Valida se JSON é FHIR válido antes de processar
 * - Processa Bundle completo (formato real da SES-GO)
 * - Código mais robusto e menos propenso a erros
 */
@Service
public class FhirParserService {

    private static final Logger logger = LoggerFactory.getLogger(FhirParserService.class);
    
    private final FhirContext fhirContext;
    private final IParser jsonParser;
    private final HemogramRepository hemogramRepository;

    public FhirParserService(FhirContext fhirContext, HemogramRepository hemogramRepository) {
        this.fhirContext = fhirContext;
        this.jsonParser = fhirContext.newJsonParser();
        this.hemogramRepository = hemogramRepository;
        
        // Configurar parser para ser mais tolerante (não falhar em extensions desconhecidas)
        jsonParser.setParserErrorHandler(new ca.uhn.fhir.parser.LenientErrorHandler());
        
        logger.info("FhirParserService inicializado com HAPI FHIR R4");
    }

    /**
     * Processa um JSON FHIR, extrai dados do hemograma e o salva no banco de dados.
     * 
     * Suporta dois formatos:
     * 1. Observation individual (código LOINC 777-3 para plaquetas)
     * 2. Bundle contendo múltiplas Observations (formato SES-GO)
     */
    public HemogramData parseFhirObservation(String fhirJson) {
        try {
            logger.debug("Iniciando parse de JSON FHIR");
            
            // Primeiro, tenta identificar o tipo de recurso
            Resource resource = jsonParser.parseResource(fhirJson);
            
            if (resource instanceof Bundle) {
                logger.info("Recurso identificado como Bundle - processando...");
                return processBundle((Bundle) resource);
            } else if (resource instanceof Observation) {
                logger.info("Recurso identificado como Observation individual - processando...");
                return processObservation((Observation) resource);
            } else {
                logger.error("Tipo de recurso FHIR não suportado: {}", resource.getResourceType());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar JSON FHIR: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Processa um Bundle FHIR (formato usado pela SES-GO).
     * Busca Observations com código LOINC 777-3 (plaquetas).
     */
    private HemogramData processBundle(Bundle bundle) {
        logger.info("Processando Bundle com {} entradas", bundle.getEntry().size());
        
        // Procurar Observation de plaquetas no Bundle
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Observation) {
                Observation obs = (Observation) entry.getResource();
                
                // Verificar se é plaquetas (LOINC 777-3)
                if (hasLoincCode(obs, ReferenceValues.PLAQUETAS_LOINC)) {
                    logger.info("Observation de plaquetas encontrada no Bundle");
                    return processObservation(obs);
                }
            }
        }
        
        logger.warn("Nenhuma Observation de plaquetas (LOINC 777-3) encontrada no Bundle");
        return null;
    }
    
    /**
     * Processa uma Observation FHIR individual e extrai dados do hemograma.
     */
    private HemogramData processObservation(Observation observation) {
        try {
            String observationId = observation.hasId() ? observation.getId() : "N/A";
            
            // Extrair referência do paciente
            String patientId = extractPatientIdFromObservation(observation);
            
            // Extrair timestamp
            LocalDateTime timestamp = extractTimestampFromObservation(observation);
            
            // Extrair região (por enquanto, fallback para Goiânia - tarefa #15 vai melhorar isso)
            String region = extractRegionFromObservation(observation);
            
            // Extrair valor das plaquetas
            Double plaquetas = extractQuantityValue(observation);
            
            // Dados do paciente (por enquanto valores padrão, JSON SES-GO não tem Patient completo)
            String patientName = "Paciente " + patientId;
            String patientCpf = extractCpfFromSubject(observation);
            String patientPhone = "Não disponível";
            
            // Por enquanto, só temos plaquetas (outros parâmetros viriam de um Bundle completo)
            Double leucocitos = null;
            Double hemoglobina = null;
            Double hematocrito = null;
            
            logger.info("Hemograma extraído: Paciente={}, Plaquetas={}", patientCpf, plaquetas);
            
            HemogramData hemogramData = new HemogramData(
                    observationId, patientId, patientName, patientCpf, patientPhone,
                    timestamp, leucocitos, hemoglobina, plaquetas, hematocrito, region
            );
            
            // Salvar no banco de dados
            saveHemogram(hemogramData);
            
            return hemogramData;
            
        } catch (Exception e) {
            logger.error("Erro ao processar Observation: {}", e.getMessage(), e);
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

    /**
     * Verifica se uma Observation tem um código LOINC específico.
     */
    private boolean hasLoincCode(Observation observation, String loincCode) {
        if (!observation.hasCode() || !observation.getCode().hasCoding()) {
            return false;
        }
        
        for (Coding coding : observation.getCode().getCoding()) {
            if ("http://loinc.org".equals(coding.getSystem()) && 
                loincCode.equals(coding.getCode())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extrai ID do paciente da Observation usando API HAPI.
     */
    private String extractPatientIdFromObservation(Observation observation) {
        if (observation.hasSubject() && observation.getSubject().hasReference()) {
            String reference = observation.getSubject().getReference();
            if (reference.startsWith("#")) {
                return reference.substring(1);
            }
            return reference.replace("Patient/", "");
        }
        return "unknown";
    }
    
    /**
     * Extrai timestamp da Observation usando API HAPI.
     */
    private LocalDateTime extractTimestampFromObservation(Observation observation) {
        try {
            Date date = null;
            
            if (observation.hasEffectiveDateTimeType()) {
                date = observation.getEffectiveDateTimeType().getValue();
            } else if (observation.hasIssued()) {
                date = observation.getIssued();
            }
            
            if (date != null) {
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            
            logger.warn("Timestamp não encontrado na Observation, usando data atual");
            return LocalDateTime.now();
            
        } catch (Exception e) {
            logger.warn("Erro ao extrair timestamp: {}. Usando data atual", e.getMessage());
            return LocalDateTime.now();
        }
    }
    
    /**
     * Extrai valor numérico (plaquetas) da Observation usando API HAPI.
     */
    private Double extractQuantityValue(Observation observation) {
        try {
            if (observation.hasValueQuantity()) {
                Quantity quantity = observation.getValueQuantity();
                if (quantity.hasValue()) {
                    return quantity.getValue().doubleValue();
                }
            }
            logger.warn("Valor numérico não encontrado na Observation");
            return null;
        } catch (Exception e) {
            logger.warn("Erro ao extrair valor: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrai CPF do subject.identifier usando API HAPI.
     */
    private String extractCpfFromSubject(Observation observation) {
        if (observation.hasSubject() && observation.getSubject().hasIdentifier()) {
            Identifier identifier = observation.getSubject().getIdentifier();
            if ("https://fhir.saude.go.gov.br/sid/cpf".equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }
        return "CPF não disponível";
    }
    
    /**
     * Extrai região da Observation.
     * TODO: Tarefa #15 vai implementar estratégia configurável (CNES, endereço, etc)
     */
    private String extractRegionFromObservation(Observation observation) {
        // Por enquanto, fallback para Goiânia
        // Tarefa #15 vai criar interface RegionResolver para extrair por CNES do performer
        logger.debug("Região não implementada ainda (aguardando tarefa #15), usando fallback");
        return "Goiânia";
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
