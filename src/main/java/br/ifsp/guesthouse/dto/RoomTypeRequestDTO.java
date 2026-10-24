package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record   RoomTypeRequestDTO(

        @NotBlank(message = "Room type name is required")
        @Size(max = 100, message = "Room type name must not exceed 100 characters")
        String name,

        @Min(value = 1, message = "Capacity must be at least 1 guest")
        int capacity,

        @NotNull(message = "Daily rate is required")
        @DecimalMin(value = "0.01", message = "Daily rate must be greater than zero")
        BigDecimal dailyRate

) {
}