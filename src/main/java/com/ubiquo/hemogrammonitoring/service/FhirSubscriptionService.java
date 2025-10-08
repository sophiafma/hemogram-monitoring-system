package com.ubiquo.hemogrammonitoring.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.ubiquo.hemogrammonitoring.config.FhirProperties;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class FhirSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FhirSubscriptionService.class);
    
    private final IGenericClient fhirClient;
    private final FhirProperties fhirProperties;
    private String subscriptionId;
    
    public FhirSubscriptionService(IGenericClient fhirClient, FhirProperties fhirProperties) {
        this.fhirClient = fhirClient;
        this.fhirProperties = fhirProperties;
    }
    
    /**
     * Cria a subscription automaticamente quando a aplicação inicia
     */
    @PostConstruct
    public void initializeSubscription() {
        if (!fhirProperties.getSubscription().isEnabled()) {
            logger.info("Subscription FHIR desabilitada na configuração");
            return;
        }
        
        try {
            logger.info("=".repeat(80));
            logger.info("🔔 INICIALIZANDO SUBSCRIPTION FHIR");
            logger.info("=".repeat(80));
            logger.info("Servidor FHIR: {}", fhirProperties.getServerUrl());
            logger.info("Callback URL: {}", fhirProperties.getSubscription().getCallbackUrl());
            logger.info("Critério: {}", fhirProperties.getSubscription().getCriteria());
            
            // Verificar se já existe uma subscription ativa
            List<Subscription> existingSubscriptions = findExistingSubscriptions();
            
            if (!existingSubscriptions.isEmpty()) {
                logger.info("✅ Subscription existente encontrada: {}", existingSubscriptions.get(0).getId());
                subscriptionId = existingSubscriptions.get(0).getIdElement().getIdPart();
            } else {
                // Criar nova subscription
                subscriptionId = createSubscription();
                logger.info("✅ Nova subscription criada com sucesso: {}", subscriptionId);
            }
            
            logger.info("=".repeat(80));
            logger.info("🎉 SUBSCRIPTION FHIR ATIVA E PRONTA PARA RECEBER NOTIFICAÇÕES!");
            logger.info("=".repeat(80));
            
        } catch (Exception e) {
            logger.error("❌ Erro ao criar/verificar subscription FHIR: {}", e.getMessage(), e);
            logger.warn("⚠️ A aplicação continuará funcionando, mas não receberá notificações automáticas");
            logger.warn("💡 Você pode testar manualmente usando o endpoint POST /fhir/subscription");
        }
    }
    
    /**
     * Busca subscriptions existentes para este callback
     */
    private List<Subscription> findExistingSubscriptions() {
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(Subscription.class)
                    .where(Subscription.URL.matches().value(fhirProperties.getSubscription().getCallbackUrl()))
                    .and(Subscription.STATUS.exactly().code("active"))
                    .returnBundle(Bundle.class)
                    .execute();
            
            return bundle.getEntry().stream()
                    .map(entry -> (Subscription) entry.getResource())
                    .toList();
                    
        } catch (Exception e) {
            logger.warn("Não foi possível buscar subscriptions existentes: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Cria uma nova subscription no servidor FHIR
     */
    private String createSubscription() {
        // Criar recurso Subscription
        Subscription subscription = new Subscription();
        
        // Status: ativo
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        
        // Motivo/descrição
        subscription.setReason("Monitoramento de hemogramas para detecção de surtos de dengue");
        
        // Critério: o que queremos monitorar
        subscription.setCriteria(fhirProperties.getSubscription().getCriteria());
        
        // Canal: rest-hook (HTTP POST)
        Subscription.SubscriptionChannelComponent channel = new Subscription.SubscriptionChannelComponent();
        channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        channel.setEndpoint(fhirProperties.getSubscription().getCallbackUrl());
        channel.setPayload("application/fhir+json");
        
        // Headers opcionais (se precisar de autenticação)
        // channel.addHeader("Authorization: Bearer token");
        
        subscription.setChannel(channel);
        
        // Enviar para o servidor FHIR
        try {
            MethodOutcome outcome = fhirClient.create()
                    .resource(subscription)
                    .execute();
            
            IdType id = (IdType) outcome.getId();
            return id.getIdPart();
            
        } catch (Exception e) {
            logger.error("Erro ao criar subscription no servidor FHIR", e);
            throw e;
        }
    }
    
    /**
     * Deleta a subscription do servidor FHIR
     */
    public void deleteSubscription() {
        if (subscriptionId != null) {
            try {
                fhirClient.delete()
                        .resourceById("Subscription", subscriptionId)
                        .execute();
                logger.info("Subscription {} deletada com sucesso", subscriptionId);
            } catch (Exception e) {
                logger.error("Erro ao deletar subscription: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Retorna o ID da subscription ativa
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }
    
    /**
     * Verifica se a subscription está ativa
     */
    public boolean isSubscriptionActive() {
        if (subscriptionId == null) {
            return false;
        }
        
        try {
            Subscription subscription = fhirClient.read()
                    .resource(Subscription.class)
                    .withId(subscriptionId)
                    .execute();
            
            return subscription.getStatus() == Subscription.SubscriptionStatus.ACTIVE;
        } catch (Exception e) {
            logger.warn("Erro ao verificar status da subscription: {}", e.getMessage());
            return false;
        }
    }
}

