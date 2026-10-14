package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomTypeRequestDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,

        @Min(value = 1, message = "A capacidade deve ser de pelo menos 1 hóspede")
        int capacity,

        @NotNull(message = "O valor da diária é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor da diária deve ser maior que zero")
        BigDecimal dailyRate

) {
}