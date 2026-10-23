package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long id,
        Long reservationId,
        PaymentType type,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {}