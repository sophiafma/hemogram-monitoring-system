package com.ubiquo.hemogrammonitoring.controller;

import com.ubiquo.hemogrammonitoring.entity.HemogramEntity;
import com.ubiquo.hemogrammonitoring.repository.HemogramRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/history")
@CrossOrigin(origins = "*")
@Tag(name = "History API", description = "Endpoints para consultar o histórico de hemogramas")
public class HemogramHistoryController {

    private final HemogramRepository hemogramRepository;

    public HemogramHistoryController(HemogramRepository hemogramRepository) {
        this.hemogramRepository = hemogramRepository;
    }

    @Operation(
            summary = "Lista todos os hemogramas salvos",
            description = "Retorna uma lista com todos os hemogramas que foram recebidos e persistidos no banco de dados."
    )
    @GetMapping("/hemograms")
    public List<HemogramEntity> getAllHemograms() {
        return hemogramRepository.findAll();
    }
}