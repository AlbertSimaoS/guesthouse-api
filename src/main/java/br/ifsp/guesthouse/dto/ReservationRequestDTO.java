package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReservationRequestDTO(
        @NotNull(message = "Room ID is required")
        @Positive(message = "Room ID must be positive")
        Long roomId,

        @NotNull(message = "Check-in date is required")
        @Future(message = "Check-in date must be in the future")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required")
        @Future(message = "Check-out date must be in the future")
        LocalDate checkOut,

        @NotNull(message = "Number of guests is required")
        @Positive(message = "Number of guests must be positive")
        Integer numberOfGuests
) {
    public boolean hasValidDateRange() {
        return checkIn != null
                && checkOut != null
                && checkOut.isAfter(checkIn);
    }
}