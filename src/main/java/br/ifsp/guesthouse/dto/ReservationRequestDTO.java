package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReservationRequestDTO(
        @NotNull(message = "RoomType ID is required")
        @Positive(message = "RoomType ID must be positive")
        Integer roomTypeId,

        @NotNull(message = "Start date is required")
        @Future(message = "Start date must be in the future")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        LocalDate endDate,

        @NotNull(message = "Number of guests is required")
        @Positive(message = "Number of guests must be positive")
        Integer numberOfGuests
) {
}