package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.NotNull;

public record CheckInRequestDTO(
        @NotNull Integer roomId
) {}