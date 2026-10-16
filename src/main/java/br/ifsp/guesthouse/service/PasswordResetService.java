package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.ForgotPasswordRequestDTO;
import br.ifsp.guesthouse.dto.PasswordResetTokenResponseDTO;
import br.ifsp.guesthouse.dto.ResetPasswordRequestDTO;
import br.ifsp.guesthouse.model.PasswordResetToken;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.repository.PasswordResetTokenRepository;
import br.ifsp.guesthouse.repository.UserAccountRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserAccountRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PasswordResetTokenResponseDTO requestPasswordReset(
            ForgotPasswordRequestDTO request) {

        UserAccount user = userRepository
                .findByCpfAndEmail(request.cpf(), request.email())
                .orElseThrow(() -> new IllegalArgumentException(
                        "CPF ou e-mail inválido."
                ));

        tokenRepository.deleteByUser_Id(user.getId());

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken(
                tokenHash,
                user,
                LocalDateTime.now().plusMinutes(15)
        );

        tokenRepository.save(resetToken);

        return new PasswordResetTokenResponseDTO(
                "Token gerado com sucesso. Ele expira em 15 minutos.",
                rawToken
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException(
                    "A nova senha e a confirmação são diferentes."
            );
        }

        String tokenHash = hashToken(request.token());

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token inválido ou expirado."
                ));

        if (resetToken.isUsed()
                || !resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Token inválido ou expirado."
            );
        }

        UserAccount user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        resetToken.markAsUsed();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Erro ao processar o token.", e
            );
        }
    }
}