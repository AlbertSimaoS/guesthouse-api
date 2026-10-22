package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.ReservationRequestDTO;
import br.ifsp.guesthouse.dto.ReservationResponseDTO;
import br.ifsp.guesthouse.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Authentication",
        description = "Endpoints for creating and managing reservations"
)
@RestController
@RequestMapping("/reservation")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(
            summary = "Create a reservation",
            description = "Creates a new reservation for the authenticated guest, validates the reservation details, and returns the created reservation information."
    )
    @PostMapping
    public ResponseEntity<ReservationResponseDTO> createReservation(
		@Valid @RequestBody 
		ReservationRequestDTO request
	) {
        ReservationResponseDTO response = reservationService.createReservation(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}