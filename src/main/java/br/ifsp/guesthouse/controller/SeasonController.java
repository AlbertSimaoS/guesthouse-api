package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.SeasonRequestDTO;
import br.ifsp.guesthouse.dto.SeasonResponseDTO;
import br.ifsp.guesthouse.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Seasons",
        description = "Endpoints for managing seasonal rates"
)
@RestController
@RequestMapping("/season")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @Operation(
            summary = "Register a new season",
            description = "Registers a new season with the provided details, including its date range and pricing multiplier, and returns the created season information. This operation requires the RECEPTION role."
    )
    @PostMapping("/register")
    public ResponseEntity<SeasonResponseDTO> register(@Valid @RequestBody SeasonRequestDTO dto) {
        SeasonResponseDTO response = seasonService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}