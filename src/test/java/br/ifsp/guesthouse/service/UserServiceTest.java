package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.RegisterRequestDTO;
import br.ifsp.guesthouse.exception.DuplicateResourceException;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService (unitário) - RF01")
class UserServiceTest {

    @Mock private UserAccountRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private RegisterRequestDTO request() {
        return new RegisterRequestDTO(
                "  Maria Silva ", "12345678901", "  Maria@Email.COM ", "+5511987654321", "senha12345");
    }

    @Test
    @DisplayName("cadastra hóspede: normaliza e-mail, aplica hash na senha e define o perfil GUEST")
    void registersGuest() {
        when(passwordEncoder.encode("senha12345")).thenReturn("HASH");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UserAccount saved = userService.register(request());

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        UserAccount persisted = captor.getValue();
        assertThat(saved).isSameAs(persisted);
        assertThat(persisted.getName()).isEqualTo("Maria Silva");
        assertThat(persisted.getEmail()).isEqualTo("maria@email.com");
        assertThat(persisted.getCpf()).isEqualTo("12345678901");
        assertThat(persisted.getPassword()).isEqualTo("HASH").isNotEqualTo("senha12345");
        assertThat(persisted.getProfile()).isEqualTo(Profile.GUEST);
    }

    @Test
    @DisplayName("e-mail já cadastrado lança DuplicateResourceException e não grava")
    void duplicateEmail() {
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("CPF já cadastrado lança DuplicateResourceException e não grava")
    void duplicateCpf() {
        when(userRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CPF");
        verify(userRepository, never()).save(any());
    }
}
