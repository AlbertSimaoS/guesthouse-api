package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.ForgotPasswordRequestDTO;
import br.ifsp.guesthouse.dto.PasswordResetTokenResponseDTO;
import br.ifsp.guesthouse.dto.ResetPasswordRequestDTO;
import br.ifsp.guesthouse.model.PasswordResetToken;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.repository.PasswordResetTokenRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PasswordResetService (unitário)")
class PasswordResetServiceTest {

    @Mock private UserAccountRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PasswordResetService service;

    private final UserAccount user = TestData.user(1L, "maria@email.com", Profile.GUEST);

    private PasswordResetToken issueToken(String[] rawOut) {
        when(userRepository.findByCpfAndEmail("12345678901", "maria@email.com")).thenReturn(Optional.of(user));
        PasswordResetTokenResponseDTO response =
                service.requestPasswordReset(new ForgotPasswordRequestDTO("12345678901", "maria@email.com"));
        rawOut[0] = response.token();
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("gera token aleatório, guarda só o hash (SHA-256) e expira em ~15 minutos")
    void issuesToken() {
        String[] raw = new String[1];
        PasswordResetToken saved = issueToken(raw);

        assertThat(raw[0]).isNotBlank();
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(raw[0]);
        assertThat(saved.isValid()).isTrue();
        assertThat(saved.getExpiresAt()).isBetween(LocalDateTime.now().plusMinutes(14), LocalDateTime.now().plusMinutes(16));
        verify(tokenRepository).deleteByUser_Id(1L);
    }

    @Test
    @DisplayName("CPF/e-mail que não conferem são rejeitados")
    void unknownAccount() {
        when(userRepository.findByCpfAndEmail(anyString(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPasswordReset(new ForgotPasswordRequestDTO("12345678901", "x@x.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("redefine a senha com o token emitido e invalida o token")
    void resetsPassword() {
        String[] raw = new String[1];
        PasswordResetToken saved = issueToken(raw);
        when(tokenRepository.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));
        when(passwordEncoder.encode("novaSenha1")).thenReturn("ENC");

        service.resetPassword(new ResetPasswordRequestDTO(raw[0], "novaSenha1", "novaSenha1"));

        assertThat(user.getPassword()).isEqualTo("ENC");
        assertThat(saved.isUsed()).isTrue();
        assertThat(saved.isValid()).isFalse();
    }

    @Test
    @DisplayName("rejeita confirmação diferente, token vazio, desconhecido, usado ou expirado")
    void resetRejections() {
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequestDTO("t", "novaSenha1", "outra")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequestDTO(" ", "novaSenha1", "novaSenha1")))
                .isInstanceOf(IllegalArgumentException.class);

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequestDTO("t", "novaSenha1", "novaSenha1")))
                .isInstanceOf(IllegalArgumentException.class);

        PasswordResetToken expired = new PasswordResetToken("h", user, LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequestDTO("t", "novaSenha1", "novaSenha1")))
                .isInstanceOf(IllegalArgumentException.class);

        PasswordResetToken used = new PasswordResetToken("h", user, LocalDateTime.now().plusMinutes(5));
        used.markAsUsed();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(used));
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequestDTO("t", "novaSenha1", "novaSenha1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
