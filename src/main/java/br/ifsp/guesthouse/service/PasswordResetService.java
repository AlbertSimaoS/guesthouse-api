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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class PasswordResetService {

    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserAccountRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PasswordResetTokenResponseDTO requestPasswordReset(
            ForgotPasswordRequestDTO request
    ) {
        UserAccount user = userRepository
                .findByCpfAndEmail(request.cpf(), request.email())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unable to process the password reset request."
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
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES)
        );

        tokenRepository.save(resetToken);

        // For development only. In production, deliver the token
        // through a verified channel such as the user's email.
        return new PasswordResetTokenResponseDTO(
                "If the provided information matches an account, "
                        + "password reset instructions will be sent.",
                rawToken
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        if (request.newPassword() == null
                || !Objects.equals(
                request.newPassword(),
                request.confirmPassword()
        )) {
            throw new IllegalArgumentException(
                    "The new password and confirmation do not match."
            );
        }

        if (request.token() == null || request.token().isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid or expired reset token."
            );
        }

        String tokenHash = hashToken(request.token());

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset token."
                ));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException(
                    "Invalid or expired reset token."
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

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Unable to process the reset token.", e
            );
        }
    }
}