package com.ubiquo.hemogrammonitoring.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hemograms")
public class HemogramEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String observationId;

    private String patientId;
    private String patientName;
    private String patientCpf;
    private String patientPhone;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Double leucocitos;
    private Double hemoglobina;
    private Double plaquetas;
    private Double hematocrito;
    private String region;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObservationId() {
        return observationId;
    }

    public void setObservationId(String observationId) {
        this.observationId = observationId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientCpf() {
        return patientCpf;
    }

    public void setPatientCpf(String patientCpf) {
        this.patientCpf = patientCpf;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
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
}
