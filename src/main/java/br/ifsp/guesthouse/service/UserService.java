
package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.UserRegistrationDTO;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(UserRegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException(
                    "Email already registered");
        }

        if (userRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException(
                    "CPF already registered");
        }

        UserAccount user = new UserAccount();

        user.setName(dto.name());
        user.setCpf(dto.cpf());
        user.setEmail(dto.email());
        user.setTelephone(dto.telephone());

        user.setPassword(
                passwordEncoder.encode(dto.password()));

        user.setProfile(Profile.GUEST);

        return userRepository.save(user);
    }
}
