package com.ubiquo.hemogrammonitoring.controller;

import com.ubiquo.hemogrammonitoring.dto.IndicadoresRegionaisDTO;
import com.ubiquo.hemogrammonitoring.service.AnaliseColetivaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para análise coletiva de hemogramas
 * Marco 4 - Detecção de padrões coletivos por região
 */
@RestController
@RequestMapping("/analise")
@CrossOrigin(origins = "*")
@Tag(name = "Análise Coletiva", description = "Endpoints para análise populacional de hemogramas por região")
public class AnaliseController {
    
    private final AnaliseColetivaService analiseColetivaService;
    
    public AnaliseController(AnaliseColetivaService analiseColetivaService) {
        this.analiseColetivaService = analiseColetivaService;
    }
    
    @Operation(
        summary = "Analisa indicadores de uma região específica",
        description = "Calcula indicadores coletivos (total de hemogramas, proporção de alertas, média de plaquetas) " +
                     "de uma região em uma janela de tempo. Detecta risco coletivo quando mais de 40% dos hemogramas " +
                     "apresentam plaquetas baixas (< 150.000/µL)."
    )
    @GetMapping("/regiao/{regiao}")
    public ResponseEntity<IndicadoresRegionaisDTO> analisarRegiao(
            @Parameter(description = "Nome da região (ex: Goiânia, Anápolis)", example = "Goiânia")
            @PathVariable String regiao,
            
            @Parameter(description = "Janela de tempo em horas (padrão: 24h)", example = "24")
            @RequestParam(defaultValue = "24") int horas
    ) {
        IndicadoresRegionaisDTO indicadores = analiseColetivaService.analisarRegiao(regiao, horas);
        return ResponseEntity.ok(indicadores);
    }
    
    @Operation(
        summary = "Lista todas as regiões cadastradas",
        description = "Retorna lista de todas as regiões que possuem hemogramas cadastrados no sistema"
    )
    @GetMapping("/regioes")
    public ResponseEntity<List<String>> listarRegioes() {
        List<String> regioes = analiseColetivaService.listarRegioes();
        return ResponseEntity.ok(regioes);
    }
    
    @Operation(
        summary = "Identifica regiões com risco coletivo de dengue",
        description = "Varre todas as regiões e retorna apenas aquelas onde mais de 40% dos hemogramas " +
                     "apresentam plaquetas baixas, indicando possível surto coletivo de dengue."
    )
    @GetMapping("/regioes-risco")
    public ResponseEntity<List<IndicadoresRegionaisDTO>> identificarRegioesComRisco(
            @Parameter(description = "Janela de tempo em horas (padrão: 24h)", example = "24")
            @RequestParam(defaultValue = "24") int horas
    ) {
        List<IndicadoresRegionaisDTO> regioesRisco = analiseColetivaService.identificarRegioesComRisco(horas);
        return ResponseEntity.ok(regioesRisco);
    }
}

