
package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount authenticate(String email, String password) {
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }

        return user;
    }
}
