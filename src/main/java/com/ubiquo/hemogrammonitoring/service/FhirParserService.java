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
            // parseResource retorna IBaseResource, então fazemos cast para Resource (R4)
            Resource resource = (Resource) jsonParser.parseResource(fhirJson);
            
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
     * Extrai TODOS os parâmetros do hemograma (leucócitos, hemoglobina, plaquetas, hematócrito).
     */
    private HemogramData processBundle(Bundle bundle) {
        logger.info("Processando Bundle com {} entradas", bundle.getEntry().size());
        
        // Variáveis para armazenar os valores extraídos
        Double leucocitos = null;
        Double hemoglobina = null;
        Double plaquetas = null;
        Double hematocrito = null;
        
        // Dados comuns (serão extraídos da primeira Observation válida)
        String observationId = null;
        String patientId = null;
        String patientCpf = null;
        LocalDateTime timestamp = null;
        String region = null;
        Observation firstObservation = null;
        
        // Processar todas as Observations do Bundle
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Observation) {
                Observation obs = (Observation) entry.getResource();
                
                // Extrair dados comuns da primeira Observation válida
                if (firstObservation == null) {
                    firstObservation = obs;
                    observationId = obs.hasId() ? obs.getId() : java.util.UUID.randomUUID().toString();
                    patientId = extractPatientIdFromObservation(obs);
                    patientCpf = extractCpfFromSubject(obs);
                    timestamp = extractTimestampFromObservation(obs);
                    region = extractRegionFromObservation(obs);
                }
                
                // Extrair valores baseado no código LOINC
                if (hasLoincCode(obs, ReferenceValues.LEUCOCITOS_LOINC)) {
                    leucocitos = extractQuantityValue(obs);
                    logger.debug("Leucócitos encontrados: {}", leucocitos);
                } else if (hasLoincCode(obs, ReferenceValues.HEMOGLOBINA_LOINC)) {
                    hemoglobina = extractQuantityValue(obs);
                    logger.debug("Hemoglobina encontrada: {}", hemoglobina);
                } else if (hasLoincCode(obs, ReferenceValues.PLAQUETAS_LOINC)) {
                    plaquetas = extractQuantityValue(obs);
                    logger.debug("Plaquetas encontradas: {}", plaquetas);
                } else if (hasLoincCode(obs, ReferenceValues.HEMATOCRITO_LOINC)) {
                    hematocrito = extractQuantityValue(obs);
                    logger.debug("Hematócrito encontrado: {}", hematocrito);
                }
            }
        }
        
        // Verificar se encontrou pelo menos um parâmetro
        if (leucocitos == null && hemoglobina == null && plaquetas == null && hematocrito == null) {
            logger.warn("Nenhum parâmetro de hemograma encontrado no Bundle");
            return null;
        }
        
        if (firstObservation == null) {
            logger.warn("Nenhuma Observation válida encontrada no Bundle");
            return null;
        }
        
        logger.info("Bundle processado: Leucócitos={}, Hemoglobina={}, Plaquetas={}, Hematócrito={}", 
                    leucocitos, hemoglobina, plaquetas, hematocrito);
        
        // Criar HemogramData com todos os valores extraídos
        String patientName = "Paciente " + patientId;
        String patientPhone = "Não disponível";
        
        HemogramData hemogramData = new HemogramData(
                observationId, patientId, patientName, patientCpf, patientPhone,
                timestamp, leucocitos, hemoglobina, plaquetas, hematocrito, region
        );
        
        // Salvar no banco de dados
        saveHemogram(hemogramData);
        
        return hemogramData;
    }
    
    /**
     * Processa uma Observation FHIR individual e extrai dados do hemograma.
     */
    private HemogramData processObservation(Observation observation) {
        try {
            String observationId = observation.hasId() ? observation.getId() : java.util.UUID.randomUUID().toString();

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
     * Extrai região/bairro da Observation.
     * 
     * Estratégias de extração (em ordem de prioridade):
     * 1. Extension com bairro/região (quando disponível no JSON gerado)
     * 2. CNES do performer (pode ser usado para mapear região)
     * 3. Endereço do paciente (se disponível)
     * 4. Fallback para "Goiânia"
     */
    private String extractRegionFromObservation(Observation observation) {
        // TODO: Quando o script de geração incluir bairro, extrair de:
        // - Extension na Observation
        // - Extension no Patient (se disponível)
        // - Address do Patient (se disponível)
        
        // Por enquanto, tenta extrair CNES do performer para log
        if (observation.hasPerformer() && !observation.getPerformer().isEmpty()) {
            Reference performerRef = observation.getPerformerFirstRep();
            if (performerRef.hasIdentifier()) {
                Identifier identifier = performerRef.getIdentifier();
                if ("https://fhir.saude.go.gov.br/sid/cnes".equals(identifier.getSystem())) {
                    String cnes = identifier.getValue();
                    logger.debug("CNES encontrado: {} (pode ser usado para mapear região no futuro)", cnes);
                }
            }
        }
        
        // Fallback: retorna "Goiânia" por enquanto
        // Quando o script de geração incluir bairro, este método será atualizado para extrair
        logger.debug("Bairro/região não encontrado no JSON, usando fallback 'Goiânia'");
        return "Goiânia";
    }

    public List<String> analyzeHemogram(HemogramData hemogram) {
        List<String> deviations = new ArrayList<>();

        // Análise de Leucócitos
        if (hemogram.getLeucocitos() != null) {
            if (!ReferenceValues.isLeucocitosNormal(hemogram.getLeucocitos())) {
                deviations.add(String.format("Leucócitos alterados: %.2f /µL (normal: %.0f-%.0f)",
                        hemogram.getLeucocitos(), ReferenceValues.LEUCOCITOS_MIN, ReferenceValues.LEUCOCITOS_MAX));
            }
        }

        // Análise de Hemoglobina
        if (hemogram.getHemoglobina() != null) {
            if (!ReferenceValues.isHemoglobinaNormal(hemogram.getHemoglobina())) {
                deviations.add(String.format("Hemoglobina alterada: %.2f g/dL (normal: %.1f-%.1f)",
                        hemogram.getHemoglobina(), ReferenceValues.HEMOGLOBINA_MIN, ReferenceValues.HEMOGLOBINA_MAX));
            }
        }

        // Análise de Plaquetas
        if (hemogram.getPlaquetas() != null) {
            if (!ReferenceValues.isPlaquetasNormal(hemogram.getPlaquetas())) {
                deviations.add(String.format("Plaquetas alteradas: %.0f /µL (normal: %.0f-%.0f)",
                        hemogram.getPlaquetas(), ReferenceValues.PLAQUETAS_MIN, ReferenceValues.PLAQUETAS_MAX));

            }
        }

        // Análise de Hematócrito
        if (hemogram.getHematocrito() != null) {
            if (!ReferenceValues.isHematocritoNormal(hemogram.getHematocrito())) {
                deviations.add(String.format("Hematócrito alterado: %.2f%% (normal: %.0f-%.0f%%)",
                        hemogram.getHematocrito(), ReferenceValues.HEMATOCRITO_MIN, ReferenceValues.HEMATOCRITO_MAX));
            }
        }

        // LÓGICA COMBINADA DE ALERTA DE DENGUE (PLAQUETAS + LEUCÓCITOS)
        boolean temPlaquetasBaixas = hemogram.getPlaquetas() != null && ReferenceValues.isPlaquetasBaixas(hemogram.getPlaquetas());
        boolean temLeucocitosBaixos = hemogram.getLeucocitos() != null && ReferenceValues.isLeucocitosBaixos(hemogram.getLeucocitos());

        // Só emite alerta de DENGUE se ambos estiverem baixos
        if (temPlaquetasBaixas && temLeucocitosBaixos) {
            deviations.add("⚠️ ALERTA DENGUE: Plaquetas E Leucócitos baixos detectados simultaneamente!");
        }

        return deviations;
    }
}
