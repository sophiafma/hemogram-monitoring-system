package com.ubiquo.hemogrammonitoring.controller;

import com.ubiquo.hemogrammonitoring.config.FhirProperties;
import com.ubiquo.hemogrammonitoring.service.FhirSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/subscription")
@CrossOrigin(origins = "*")
@Tag(name = "Subscription Management", description = "Gerenciamento de Subscriptions FHIR")
public class SubscriptionController {
    
    private final FhirSubscriptionService subscriptionService;
    private final FhirProperties fhirProperties;
    
    public SubscriptionController(FhirSubscriptionService subscriptionService, FhirProperties fhirProperties) {
        this.subscriptionService = subscriptionService;
        this.fhirProperties = fhirProperties;
    }
    
    @Operation(
        summary = "Verifica status da Subscription FHIR",
        description = "Retorna informações sobre a subscription ativa no servidor FHIR"
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus() {
        Map<String, Object> response = new HashMap<>();
        
        String subscriptionId = subscriptionService.getSubscriptionId();
        boolean isActive = subscriptionService.isSubscriptionActive();
        
        response.put("subscriptionEnabled", fhirProperties.getSubscription().isEnabled());
        response.put("fhirServerUrl", fhirProperties.getServerUrl());
        response.put("callbackUrl", fhirProperties.getSubscription().getCallbackUrl());
        response.put("criteria", fhirProperties.getSubscription().getCriteria());
        response.put("subscriptionId", subscriptionId);
        response.put("isActive", isActive);
        
        if (subscriptionId != null && isActive) {
            response.put("status", "active");
            response.put("message", "Subscription ativa e pronta para receber notificações do servidor FHIR");
        } else if (subscriptionId != null && !isActive) {
            response.put("status", "inactive");
            response.put("message", "Subscription existe mas não está ativa");
        } else {
            response.put("status", "not_created");
            response.put("message", "Subscription não foi criada (possível erro de conexão com servidor FHIR)");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Recria a Subscription FHIR",
        description = "Deleta a subscription existente (se houver) e cria uma nova"
    )
    @PostMapping("/recreate")
    public ResponseEntity<Map<String, Object>> recreateSubscription() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            subscriptionService.deleteSubscription();
            subscriptionService.initializeSubscription();
            
            response.put("status", "success");
            response.put("message", "Subscription recriada com sucesso");
            response.put("subscriptionId", subscriptionService.getSubscriptionId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Erro ao recriar subscription: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

