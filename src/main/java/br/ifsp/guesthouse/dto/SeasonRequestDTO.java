package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonRequestDTO(

        @NotBlank(message = "Season name is required")
        @Size(max = 100, message = "Season name must not exceed 100 characters")
        String name,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Multiplier is required")
        @DecimalMin(value = "0.01", message = "Multiplier must be greater than zero")
        BigDecimal multiplier

) {
}