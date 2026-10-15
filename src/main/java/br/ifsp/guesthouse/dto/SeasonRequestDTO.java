package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonRequestDTO(

        @NotBlank(message = "O nome da temporada é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @NotNull(message = "A data de início é obrigatória")
        LocalDate startDate,

        @NotNull(message = "A data de término é obrigatória")
        LocalDate endDate,

        @NotNull(message = "O multiplicador é obrigatório")
        @DecimalMin(value = "0.01", message = "O multiplicador deve ser maior que zero")
        BigDecimal multiplier

) {
}