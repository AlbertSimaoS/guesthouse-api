package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponseDTO(
        Long id,
        Integer roomTypeId,
        Integer roomId,
        Long userId,
        LocalDate startDate,
        LocalDate endDate,
        Integer numberOfGuests,
        BigDecimal totalPrice,
        BigDecimal depositAmount,
        ReservationStatus status
) {}