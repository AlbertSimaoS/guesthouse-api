package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReservationRequestDTO(
        @NotNull
        @Positive
        Long roomId,

        @NotNull
        @Future
        LocalDate checkIn,

        @NotNull
        @Future
        LocalDate checkOut,

        @NotNull
        @Positive
        Integer numberOfGuests
) {
    public boolean hasValidDateRange() {
        return checkIn != null
                && checkOut != null
                && checkOut.isAfter(checkIn);
    }
}