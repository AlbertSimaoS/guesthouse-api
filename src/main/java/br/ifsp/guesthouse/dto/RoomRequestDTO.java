package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.*;

public record RoomRequestDTO (
        @NotBlank(message = "Room number is required")
        @Size(max = 3, message = "Room number must contain at most 3 characters")
        @Pattern(regexp = "\\d+", message = "Room number must contain only digits")
        String number,

        @NotNull(message = "Room type is required")
        @Positive(message = "Room type ID must be positive")
        Long typeId
) {}
