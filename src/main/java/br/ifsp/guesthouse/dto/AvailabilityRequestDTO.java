package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AvailabilityRequestDTO(

        @NotNull(message = "Start Date is required")
        @Future(message = "Start Date date must be in the future")
        LocalDate startDate,

        @NotNull(message = "End Date is required")
        @Future(message = "End Date date must be in the future")
        LocalDate endDate,

        @NotNull(message = "Number of guests is required")
        @Positive(message = "Number of guests must be positive")
        Integer numberOfGuests
) {
}