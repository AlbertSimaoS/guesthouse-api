package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.PaymentResponseDTO;
import br.ifsp.guesthouse.dto.PaymentUpdateRequestDTO;
import br.ifsp.guesthouse.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Payments",
        description = "Endpoints for managing payments"
)
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final ReservationService reservationService;

    public PaymentController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(
            summary = "Update payment status",
            description = "Updates a pending payment status. When a deposit is confirmed, the associated reservation is also confirmed."
    )
    @PatchMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentUpdateRequestDTO request) {

        PaymentResponseDTO response =
                reservationService.updateReservationStatus(
                        id,
                        request.status()
                );

        return ResponseEntity.ok(response);
    }
}