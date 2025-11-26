package com.ubiquo.hemogrammonitoring.controller;

import com.ubiquo.hemogrammonitoring.model.HemogramData;
import com.ubiquo.hemogrammonitoring.service.FhirParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fhir")
@CrossOrigin(origins = "*")
@Tag(name = "FHIR Hemogram API", description = "Endpoints para recepção e processamento de hemogramas FHIR")
public class FhirController {
    
    private static final Logger logger = LoggerFactory.getLogger(FhirController.class);
    
    @Autowired
    private FhirParserService fhirParserService;

    @Operation(
            summary = "Testa o parser diretamente",
            description = "Endpoint para enviar um JSON FHIR diretamente para a aplicação, sem passar pelo servidor FHIR. Útil para depurar o parser."
    )
    @PostMapping("/direct-test")
    public ResponseEntity<Map<String, Object>> testParserDirectly(@RequestBody String fhirJson) {
        logger.info("=".repeat(80));
        logger.info("📨 TESTE DIRETO RECEBIDO");
        logger.info("=".repeat(80));

        // Esta chamada reutiliza a mesma lógica do endpoint de subscription
        return receiveFhirData(fhirJson);
    }

        @Operation(
        summary = "Recebe dados FHIR via subscription",
        description = "Endpoint para receber hemogramas em formato FHIR dos laboratórios. " +
                     "Processa o JSON, extrai os valores dos parâmetros hematológicos usando códigos LOINC " +
                     "e detecta automaticamente alertas de dengue baseado em plaquetas baixas."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dados processados com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro ao processar dados FHIR"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping("/subscription")
    public ResponseEntity<Map<String, Object>> receiveFhirData(
            @Parameter(description = "JSON FHIR contendo dados do hemograma (recurso Observation)", required = true,
                      content = @Content(examples = @ExampleObject(value = """
                          {
                            "resourceType": "Observation",
                            "id": "hemograma-001",
                            "status": "final",
                            "subject": {"reference": "Patient/patient-001"},
                            "effectiveDateTime": "2025-10-07T15:30:00",
                            "component": [
                              {
                                "code": {"coding": [{"system": "http://loinc.org", "code": "777-3"}]},
                                "valueQuantity": {"value": 80000, "unit": "/µL"}
                              }
                            ]
                          }
                          """)))
            @RequestBody String fhirJson) {
        logger.info("=".repeat(80));
        logger.info("📨 NOTIFICAÇÃO RECEBIDA DO SERVIDOR FHIR VIA SUBSCRIPTION");
        logger.info("=".repeat(80));
        logger.debug("JSON recebido: {}", fhirJson);
        
        try {
            // Processar o JSON FHIR
            HemogramData hemogramData = fhirParserService.parseFhirObservation(fhirJson);
            
            if (hemogramData != null) {
                // Analisar o hemograma
                List<String> deviations = fhirParserService.analyzeHemogram(hemogramData);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Dados FHIR processados com sucesso");
                response.put("hemogramData", hemogramData);
                response.put("deviations", deviations);
                response.put("hasDengueAlert", deviations.stream().anyMatch(d -> d.contains("ALERTA DENGUE")));
                
                logger.info("Dados processados com sucesso. Desvios encontrados: {}", deviations.size());
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Erro ao processar dados FHIR");
                
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar dados FHIR: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro interno: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(
        summary = "Testa o sistema com dados mockados",
        description = "Endpoint de teste que processa um hemograma mockado com plaquetas baixas (120.000 /µL) " +
                     "para demonstrar a detecção automática de alerta de dengue. " +
                     "Útil para verificar se o sistema está funcionando corretamente."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Teste executado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Erro ao processar dados mockados"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping("/test-hemograma")
    public ResponseEntity<Map<String, Object>> testHemograma() {
        logger.info("Executando teste do sistema com dados mockados...");
        
        try {
            // Dados FHIR mockados para teste
            String mockFhirJson = createMockFhirJson();
            
            // Processar os dados mockados
            HemogramData hemogramData = fhirParserService.parseFhirObservation(mockFhirJson);
            
            if (hemogramData != null) {
                // Analisar o hemograma
                List<String> deviations = fhirParserService.analyzeHemogram(hemogramData);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Teste executado com sucesso");
                response.put("mockFhirJson", mockFhirJson);
                response.put("hemogramData", hemogramData);
                response.put("deviations", deviations);
                response.put("hasDengueAlert", deviations.stream().anyMatch(d -> d.contains("ALERTA DENGUE")));
                
                logger.info("Teste executado com sucesso. Desvios encontrados: {}", deviations.size());
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Erro ao processar dados mockados");
                
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Erro no teste: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Erro interno: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Cria dados FHIR mockados para teste
     */
    private String createMockFhirJson() {
        return """
        {
            "resourceType": "Observation",
            "id": "hemograma-001",
            "status": "final",
            "category": [
                {
                    "coding": [
                        {
                            "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                            "code": "laboratory",
                            "display": "Laboratory"
                        }
                    ]
                }
            ],
            "code": {
                "coding": [
                    {
                        "system": "http://loinc.org",
                        "code": "58410-2",
                        "display": "Complete blood count (hemogram) panel"
                    }
                ]
            },
            "subject": {
                "reference": "Patient/patient-001"
            },
            "effectiveDateTime": "2024-01-15T10:30:00",
            "issued": "2024-01-15T10:35:00",
            "component": [
                {
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "33747-0",
                                "display": "Leukocytes [#/volume] in Blood"
                            }
                        ]
                    },
                    "valueQuantity": {
                        "value": 8500.0,
                        "unit": "/µL",
                        "system": "http://unitsofmeasure.org",
                        "code": "/uL"
                    }
                },
                {
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "718-7",
                                "display": "Hemoglobin [Mass/volume] in Blood"
                            }
                        ]
                    },
                    "valueQuantity": {
                        "value": 14.2,
                        "unit": "g/dL",
                        "system": "http://unitsofmeasure.org",
                        "code": "g/dL"
                    }
                },
                {
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "777-3",
                                "display": "Platelets [#/volume] in Blood"
                            }
                        ]
                    },
                    "valueQuantity": {
                        "value": 120000.0,
                        "unit": "/µL",
                        "system": "http://unitsofmeasure.org",
                        "code": "/uL"
                    }
                },
                {
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "4544-3",
                                "display": "Hematocrit [Volume Fraction] of Blood"
                            }
                        ]
                    },
                    "valueQuantity": {
                        "value": 42.5,
                        "unit": "%",
                        "system": "http://unitsofmeasure.org",
                        "code": "%"
                    }
                }
            ]
        }
        """;
    }
}
