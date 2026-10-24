package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentUpdateRequestDTO(
        @NotNull(message = "Payment status is required")
        PaymentStatus status
) {}