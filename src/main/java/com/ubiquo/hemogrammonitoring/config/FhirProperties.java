package com.ubiquo.hemogrammonitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hemogram.monitoring.fhir")
public class FhirProperties {
    
    private String serverUrl;
    private Subscription subscription = new Subscription();
    
    public String getServerUrl() {
        return serverUrl;
    }
    
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public Subscription getSubscription() {
        return subscription;
    }
    
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
    
    public static class Subscription {
        private boolean enabled = true;
        private String callbackUrl;
        private String criteria;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getCallbackUrl() {
            return callbackUrl;
        }
        
        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }
        
        public String getCriteria() {
            return criteria;
        }
        
        public void setCriteria(String criteria) {
            this.criteria = criteria;
        }
    }
}

