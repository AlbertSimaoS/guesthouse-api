package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.ConsumptionRequestDTO;
import br.ifsp.guesthouse.dto.ConsumptionResponseDTO;
import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.service.ConsumptionService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations/{reservationId}/consumptions")
public class ConsumptionController {

    private final ConsumptionService consumptionService;

    public ConsumptionController(ConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }

    @PostMapping
    public ResponseEntity<ConsumptionResponseDTO> createConsumption(
            @PathVariable Long reservationId,
            @Valid @RequestBody ConsumptionRequestDTO request
    ) {

        ConsumptionResponseDTO response =
                consumptionService.createConsumption(
                        reservationId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ConsumptionResponseDTO>> getConsumptions(
            @PathVariable Long reservationId,
            @PageableDefault(
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        return ResponseEntity.ok(
                consumptionService.getConsumptionsByReservation(
                        reservationId,
                        pageable
                )
        );
    }
}