package com.ubiquo.hemogrammonitoring.dto;

import java.time.LocalDateTime;

/**
 * DTO com indicadores da análise coletiva de uma região
 */
public class IndicadoresRegionaisDTO {
    
    private String regiao;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private long totalHemogramas;
    private long totalAlertas;
    private double proporcaoAlertas; // 0 a 1 (ex: 0.40 = 40%)
    private Double mediaPlaquetas;
    private Double mediaLeucocitos;
    private boolean temRiscoColetivo; // true se proporção > 40%
    private String mensagem;
    private String tendenciaPlaquetas; // "SUBINDO", "CAINDO", "ESTAVEL"
    private Double variacaoPlaquetasPorcentagem;
    private String tendenciaLeucocitos; // "SUBINDO", "CAINDO", "ESTAVEL"
    private Double variacaoLeucocitosPorcentagem;

    public IndicadoresRegionaisDTO() {
    }
    
    public IndicadoresRegionaisDTO(String regiao, LocalDateTime dataInicio, LocalDateTime dataFim, 
                                   long totalHemogramas, long totalAlertas, Double mediaPlaquetas, Double mediaLeucocitos) {
        this.regiao = regiao;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.totalHemogramas = totalHemogramas;
        this.totalAlertas = totalAlertas;
        this.mediaPlaquetas = mediaPlaquetas;
        this.mediaLeucocitos = mediaLeucocitos;
        
        // Calcular proporção
        if (totalHemogramas > 0) {
            this.proporcaoAlertas = (double) totalAlertas / totalHemogramas;
        } else {
            this.proporcaoAlertas = 0.0;
        }
        
        // Detectar risco coletivo (>40% com alerta)
        this.temRiscoColetivo = this.proporcaoAlertas > 0.40;
        
        // Gerar mensagem
        if (totalHemogramas == 0) {
            this.mensagem = "Não há hemogramas registrados nesta região e período.";
        } else if (temRiscoColetivo) {
            this.mensagem = String.format("⚠️ ALERTA: %.1f%% dos hemogramas apresentam plaquetas baixas. Possível surto de dengue!", 
                                         proporcaoAlertas * 100);
        } else {
            this.mensagem = String.format("✅ Situação controlada: %.1f%% dos hemogramas com alteração.", 
                                         proporcaoAlertas * 100);
        }
    }

    // Getters e Setters
    
    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public long getTotalHemogramas() {
        return totalHemogramas;
    }

    public void setTotalHemogramas(long totalHemogramas) {
        this.totalHemogramas = totalHemogramas;
    }

    public long getTotalAlertas() {
        return totalAlertas;
    }

    public void setTotalAlertas(long totalAlertas) {
        this.totalAlertas = totalAlertas;
    }

    public double getProporcaoAlertas() {
        return proporcaoAlertas;
    }

    public void setProporcaoAlertas(double proporcaoAlertas) {
        this.proporcaoAlertas = proporcaoAlertas;
    }

    public Double getMediaPlaquetas() {
        return mediaPlaquetas;
    }

    public void setMediaPlaquetas(Double mediaPlaquetas) {
        this.mediaPlaquetas = mediaPlaquetas;
    }

    public Double getMediaLeucocitos() {
        return mediaLeucocitos;
    }

    public void setMediaLeucocitos(Double mediaLeucocitos) {
        this.mediaLeucocitos = mediaLeucocitos;
    }

    public boolean isTemRiscoColetivo() {
        return temRiscoColetivo;
    }

    public void setTemRiscoColetivo(boolean temRiscoColetivo) {
        this.temRiscoColetivo = temRiscoColetivo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
    public String getTendenciaPlaquetas() {
        return tendenciaPlaquetas;
    }

    public void setTendenciaPlaquetas(String tendenciaPlaquetas) {
        this.tendenciaPlaquetas = tendenciaPlaquetas;
    }

    public Double getVariacaoPlaquetasPorcentagem() {
        return variacaoPlaquetasPorcentagem;
    }

    public void setVariacaoPlaquetasPorcentagem(Double variacaoPlaquetasPorcentagem) {
        this.variacaoPlaquetasPorcentagem = variacaoPlaquetasPorcentagem;
    }

    public String getTendenciaLeucocitos() {
        return tendenciaLeucocitos;
    }

    public void setTendenciaLeucocitos(String tendenciaLeucocitos) {
        this.tendenciaLeucocitos = tendenciaLeucocitos;
    }

    public Double getVariacaoLeucocitosPorcentagem() {
        return variacaoLeucocitosPorcentagem;
    }

    public void setVariacaoLeucocitosPorcentagem(Double variacaoLeucocitosPorcentagem) {
        this.variacaoLeucocitosPorcentagem = variacaoLeucocitosPorcentagem;
    }
}

