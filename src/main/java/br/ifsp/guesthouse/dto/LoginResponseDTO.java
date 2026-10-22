
package br.ifsp.guesthouse.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        RegisterResponseDTO user
) {}
