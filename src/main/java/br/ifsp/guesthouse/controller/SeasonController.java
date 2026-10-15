package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.SeasonRequestDTO;
import br.ifsp.guesthouse.dto.SeasonResponseDTO;
import br.ifsp.guesthouse.service.SeasonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/season")
@Tag(name = "Temporadas", description = "Operações relacionadas às temporadas")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @PostMapping("/register")
    public ResponseEntity<SeasonResponseDTO> register(@Valid @RequestBody SeasonRequestDTO dto) {
        SeasonResponseDTO response = seasonService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}