
package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String telephone,
        Profile profile
) {
    public static UserResponseDTO fromEntity(UserAccount user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTelephone(),
                user.getProfile()
        );
    }
}
