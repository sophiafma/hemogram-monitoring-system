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

    private static final double MARGEM_ESTABILIDADE_PERCENTUAL = 5.0;
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

        /** Janela anterior, para comparar tendência*/
        LocalDateTime inicioAnterior = inicio.minusHours(horas);

        logger.info("Analisando região: {} | Janela: últimas {}h ({} até {})", 
                   regiao, horas, inicio, agora);
        
        // Buscar dados usando as queries do repository
        long totalHemogramas = hemogramRepository.countByRegionAndTimestampBetween(
            regiao, inicio, agora
        );
        
        long totalAlertas = hemogramRepository.countAlertsInRegionByTime(
            regiao, inicio, agora, ReferenceValues.PLAQUETAS_MIN
        );

        /** Medias atuais*/

        Double mediaPlaquetasAtual = hemogramRepository.calculateAveragePlaquetasInRegion(
            regiao, inicio, agora
        );

        Double mediaLeucocitosAtual = hemogramRepository.calculateAverageLeucocitosInRegion(
                regiao, inicio, agora
        );

        // Médias da Janela Anterior (comparação)
        Double mediaPlaquetasAnterior = hemogramRepository.calculateAveragePlaquetasInRegion(
                regiao, inicioAnterior, inicio
        );
        Double mediaLeucocitosAnterior = hemogramRepository.calculateAverageLeucocitosInRegion(
                regiao, inicioAnterior, inicio
        );

        // Criar DTO com indicadores
        IndicadoresRegionaisDTO indicadores = new IndicadoresRegionaisDTO(
                regiao, inicio, agora, totalHemogramas, totalAlertas,
                mediaPlaquetasAtual, mediaLeucocitosAtual
        );

        // Calcular e Definir Tendências
        definirTendencias(indicadores, mediaPlaquetasAtual, mediaPlaquetasAnterior,
                mediaLeucocitosAtual, mediaLeucocitosAnterior);
        
        logger.info("Resultado: {} hemogramas | {} alertas | Proporção: {:.1f}% | Risco: {}", 
                   totalHemogramas, totalAlertas, 
                   indicadores.getProporcaoAlertas() * 100,
                   indicadores.isTemRiscoColetivo() ? "SIM" : "NÃO");
        
        return indicadores;
    }

    /**
     * Método auxiliar para calcular variação percentual e definir strings de tendência
     */
    private void definirTendencias(IndicadoresRegionaisDTO dto,
                                   Double plaqAtual, Double plaqAnterior,
                                   Double leucoAtual, Double leucoAnterior) {

        // Tendência de Plaquetas
        if (plaqAtual != null && plaqAnterior != null && plaqAnterior > 0) {
            double variacao = ((plaqAtual - plaqAnterior) / plaqAnterior) * 100.0;
            dto.setVariacaoPlaquetasPorcentagem(variacao);
            dto.setTendenciaPlaquetas(calcularStatusTendencia(variacao));
        } else {
            dto.setTendenciaPlaquetas("DADOS INSUFICIENTES");
        }

        // Tendência de Leucócitos
        if (leucoAtual != null && leucoAnterior != null && leucoAnterior > 0) {
            double variacao = ((leucoAtual - leucoAnterior) / leucoAnterior) * 100.0;
            dto.setVariacaoLeucocitosPorcentagem(variacao);
            dto.setTendenciaLeucocitos(calcularStatusTendencia(variacao));
        } else {
            dto.setTendenciaLeucocitos("DADOS INSUFICIENTES");
        }
    }

    private String calcularStatusTendencia(double variacao) {
        if (variacao > MARGEM_ESTABILIDADE_PERCENTUAL) {
            return "SUBINDO 📈";
        } else if (variacao < -MARGEM_ESTABILIDADE_PERCENTUAL) {
            return "CAINDO 📉";
        } else {
            return "ESTÁVEL ➡️";
        }
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

