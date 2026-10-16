package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record RoomAvailabilityRequestDTO(

        @NotNull(message = "A data de check-in é obrigatória")
        @Future(message = "O check-in deve ser uma data futura")
        LocalDate checkIn,

        @NotNull(message = "A data de check-out é obrigatória")
        @Future(message = "O check-out deve ser uma data futura")
        LocalDate checkOut,

        @NotNull(message = "O número de hóspedes é obrigatório")
        @PositiveOrZero(message = "O número de hóspedes não pode ser negativo")
        Integer guests
) {
}