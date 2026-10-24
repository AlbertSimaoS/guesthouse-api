package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReservationRequestDTO(
        @NotNull(message = "RoomType ID is required")
        @Positive(message = "RoomType ID must be positive")
        Integer roomTypeId,

        @NotNull(message = "Check-in date is required")
        @Future(message = "Check-in date must be in the future")
        LocalDate startDate,

        @NotNull(message = "End date date is required")
        @Future(message = "End date date must be in the future")
        LocalDate endDate,

        @NotNull(message = "Number of guests is required")
        @Positive(message = "Number of guests must be positive")
        Integer numberOfGuests
) {
}