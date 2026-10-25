package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService (unitário) - RF02")
class AuthServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("credenciais válidas devolvem o usuário")
    void validCredentials() {
        UserAccount user = TestData.user(1L, "maria@email.com", Profile.GUEST);
        when(userAccountRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha12345", "hash")).thenReturn(true);

        assertThat(authService.authenticate("maria@email.com", "senha12345")).isSameAs(user);
    }

    @Test
    @DisplayName("e-mail inexistente e senha errada falham com a mesma mensagem")
    void invalidCredentialsHaveTheSameMessage() {
        UserAccount user = TestData.user(1L, "maria@email.com", Profile.GUEST);
        when(userAccountRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(userAccountRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        Throwable unknownEmail = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.authenticate("ninguem@email.com", "senha12345"));
        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> authService.authenticate("maria@email.com", "errada"));

        assertThat(unknownEmail).isInstanceOf(BadCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(BadCredentialsException.class);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThatThrownBy(() -> authService.authenticate("maria@email.com", "errada"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
