package com.ubiquo.hemogrammonitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class HemogramData {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("patientId")
    private String patientId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("leucocitos")
    private Double leucocitos; // /µL
    
    @JsonProperty("hemoglobina")
    private Double hemoglobina; // g/dL
    
    @JsonProperty("plaquetas")
    private Double plaquetas; // /µL
    
    @JsonProperty("hematocrito")
    private Double hematocrito; // %
    
    @JsonProperty("region")
    private String region; // Região geográfica
    
    // Construtores
    public HemogramData() {}
    
    public HemogramData(String id, String patientId, LocalDateTime timestamp, 
                       Double leucocitos, Double hemoglobina, Double plaquetas, 
                       Double hematocrito, String region) {
        this.id = id;
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.leucocitos = leucocitos;
        this.hemoglobina = hemoglobina;
        this.plaquetas = plaquetas;
        this.hematocrito = hematocrito;
        this.region = region;
    }
    
    // Getters e Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Double getLeucocitos() {
        return leucocitos;
    }
    
    public void setLeucocitos(Double leucocitos) {
        this.leucocitos = leucocitos;
    }
    
    public Double getHemoglobina() {
        return hemoglobina;
    }
    
    public void setHemoglobina(Double hemoglobina) {
        this.hemoglobina = hemoglobina;
    }
    
    public Double getPlaquetas() {
        return plaquetas;
    }
    
    public void setPlaquetas(Double plaquetas) {
        this.plaquetas = plaquetas;
    }
    
    public Double getHematocrito() {
        return hematocrito;
    }
    
    public void setHematocrito(Double hematocrito) {
        this.hematocrito = hematocrito;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    @Override
    public String toString() {
        return "HemogramData{" +
                "id='" + id + '\'' +
                ", patientId='" + patientId + '\'' +
                ", timestamp=" + timestamp +
                ", leucocitos=" + leucocitos +
                ", hemoglobina=" + hemoglobina +
                ", plaquetas=" + plaquetas +
                ", hematocrito=" + hematocrito +
                ", region='" + region + '\'' +
                '}';
    }
}
