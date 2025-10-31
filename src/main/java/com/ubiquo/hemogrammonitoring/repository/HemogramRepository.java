package com.ubiquo.hemogrammonitoring.repository;

import com.ubiquo.hemogrammonitoring.entity.HemogramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HemogramRepository extends JpaRepository<HemogramEntity, Long> {
    
    /**
     * Busca todos os hemogramas de uma região específica
     */
    List<HemogramEntity> findByRegion(String region);
    
    /**
     * Busca hemogramas de uma região dentro de uma janela de tempo
     * Ex: últimas 24 horas
     */
    List<HemogramEntity> findByRegionAndTimestampBetween(
            String region, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    /**
     * Busca hemogramas de uma região após uma data específica
     * Útil para janelas deslizantes (ex: últimas 24h)
     */
    List<HemogramEntity> findByRegionAndTimestampAfter(
            String region, 
            LocalDateTime after
    );
    
    /**
     * Conta quantos hemogramas existem em uma região e janela de tempo
     */
    long countByRegionAndTimestampBetween(
            String region, 
            LocalDateTime startTime, 
            LocalDateTime endTime
    );
    
    /**
     * Busca hemogramas com plaquetas baixas (< 150.000) 
     * em uma região e janela de tempo - alerta de dengue
     */
    @Query("SELECT h FROM HemogramEntity h WHERE h.region = :region " +
           "AND h.timestamp BETWEEN :startTime AND :endTime " +
           "AND h.plaquetas < :plaquetasThreshold")
    List<HemogramEntity> findAlertsInRegionByTime(
            @Param("region") String region,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("plaquetasThreshold") Double plaquetasThreshold
    );
    
    /**
     * Conta hemogramas com alerta de dengue em uma região e período
     */
    @Query("SELECT COUNT(h) FROM HemogramEntity h WHERE h.region = :region " +
           "AND h.timestamp BETWEEN :startTime AND :endTime " +
           "AND h.plaquetas < :plaquetasThreshold")
    long countAlertsInRegionByTime(
            @Param("region") String region,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("plaquetasThreshold") Double plaquetasThreshold
    );
    
    /**
     * Busca todas as regiões distintas que têm hemogramas cadastrados
     * Útil para varrer todas as regiões na análise coletiva
     */
    @Query("SELECT DISTINCT h.region FROM HemogramEntity h WHERE h.region IS NOT NULL")
    List<String> findDistinctRegions();
    
    /**
     * Calcula a média de plaquetas de uma região em um período
     */
    @Query("SELECT AVG(h.plaquetas) FROM HemogramEntity h WHERE h.region = :region " +
           "AND h.timestamp BETWEEN :startTime AND :endTime " +
           "AND h.plaquetas IS NOT NULL")
    Double calculateAveragePlaquetasInRegion(
            @Param("region") String region,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
