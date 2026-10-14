package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequestDTO (
        @NotBlank(message = "O numero do quarto é obrigatório")
        @Size(max = 3, message = "O numero do quarto so pode ter 3 digitos")
        String number,

        @NotNull(message = "O tipo do quarto é obrigatório")
        Long typeId
) {}
