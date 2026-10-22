package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.RegisterRequestDTO;
import br.ifsp.guesthouse.exception.DuplicateResourceException;
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

    public UserService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(RegisterRequestDTO dto) {

        String email = dto.email().trim().toLowerCase();
        String cpf = dto.cpf().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email is already registered."
            );
        }

        if (userRepository.existsByCpf(cpf)) {
            throw new DuplicateResourceException(
                    "CPF is already registered."
            );
        }

        UserAccount user = new UserAccount();

        user.setName(dto.name().trim());
        user.setCpf(cpf);
        user.setEmail(email);
        user.setTelephone(dto.telephone());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setProfile(Profile.GUEST);

        return userRepository.save(user);
    }
}