package com.ubiquo.hemogrammonitoring.service;

import com.ubiquo.hemogrammonitoring.dto.IndicadoresRegionaisDTO;
import com.ubiquo.hemogrammonitoring.model.ReferenceValues;
import com.ubiquo.hemogrammonitoring.repository.HemogramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service para análise coletiva de hemogramas
 * Marco 4 - Detecção de padrões coletivos
 */
@Service
public class AnaliseColetivaService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnaliseColetivaService.class);
    
    private final HemogramRepository hemogramRepository;
    
    public AnaliseColetivaService(HemogramRepository hemogramRepository) {
        this.hemogramRepository = hemogramRepository;
    }
    
    /**
     * Analisa os indicadores de uma região em uma janela de tempo
     * 
     * @param regiao Nome da região (ex: "Goiânia", "Anápolis")
     * @param horas Janela de tempo em horas (ex: 24 para últimas 24h)
     * @return Indicadores calculados
     */
    public IndicadoresRegionaisDTO analisarRegiao(String regiao, int horas) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.minusHours(horas);
        
        logger.info("Analisando região: {} | Janela: últimas {}h ({} até {})", 
                   regiao, horas, inicio, agora);
        
        // Buscar dados usando as queries do repository
        long totalHemogramas = hemogramRepository.countByRegionAndTimestampBetween(
            regiao, inicio, agora
        );
        
        long totalAlertas = hemogramRepository.countAlertsInRegionByTime(
            regiao, inicio, agora, ReferenceValues.PLAQUETAS_MIN
        );
        
        Double mediaPlaquetas = hemogramRepository.calculateAveragePlaquetasInRegion(
            regiao, inicio, agora
        );
        
        // Criar DTO com indicadores
        IndicadoresRegionaisDTO indicadores = new IndicadoresRegionaisDTO(
            regiao, inicio, agora, totalHemogramas, totalAlertas, mediaPlaquetas
        );
        
        logger.info("Resultado: {} hemogramas | {} alertas | Proporção: {:.1f}% | Risco: {}", 
                   totalHemogramas, totalAlertas, 
                   indicadores.getProporcaoAlertas() * 100,
                   indicadores.isTemRiscoColetivo() ? "SIM" : "NÃO");
        
        return indicadores;
    }
    
    /**
     * Lista todas as regiões que têm hemogramas cadastrados
     */
    public List<String> listarRegioes() {
        return hemogramRepository.findDistinctRegions();
    }
    
    /**
     * Analisa todas as regiões e retorna apenas as que têm risco coletivo
     * 
     * @param horas Janela de tempo em horas
     * @return Lista de regiões com risco
     */
    public List<IndicadoresRegionaisDTO> identificarRegioesComRisco(int horas) {
        logger.info("Identificando regiões com risco coletivo (janela: {}h)", horas);
        
        List<String> regioes = listarRegioes();
        
        return regioes.stream()
                .map(regiao -> analisarRegiao(regiao, horas))
                .filter(IndicadoresRegionaisDTO::isTemRiscoColetivo)
                .toList();
    }
}

