
package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;

public record RegisterResponseDTO(
        Long id,
        String name,
        String email,
        String telephone,
        Profile profile
) {
    public static RegisterResponseDTO fromEntity(UserAccount user) {
        return new RegisterResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTelephone(),
                user.getProfile()
        );
    }
}
