
package br.ifsp.guesthouse.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        UserResponseDTO user
) {}
