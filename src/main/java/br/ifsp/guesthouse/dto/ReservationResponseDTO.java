package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponseDTO(
        Long id,
        Integer roomId,
        Long userId,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status
) {}