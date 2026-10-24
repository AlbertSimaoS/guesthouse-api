package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.UserAccount;

public record LoginResponseDTO(
        String accessToken,
        RegisterResponseDTO user
) {
    public static LoginResponseDTO fromEntity(
            String accessToken,
            UserAccount user
    ) {
        return new LoginResponseDTO(
                accessToken,
                RegisterResponseDTO.fromEntity(user)
        );
    }
}