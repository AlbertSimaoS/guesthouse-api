package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.ForgotPasswordRequestDTO;
import br.ifsp.guesthouse.dto.PasswordResetTokenResponseDTO;
import br.ifsp.guesthouse.dto.ResetPasswordRequestDTO;
import br.ifsp.guesthouse.service.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(
        name = "Password Reset",
        description = "Endpoints for password recovery, reset and change"
)
@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(
            summary = "Request password recovery",
            description = "Initiates the password recovery process and returns the password reset token."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetTokenResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        PasswordResetTokenResponseDTO response = passwordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset user password",
            description = "Resets the user's password using a valid password reset token and returns a confirmation message."
    )
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Senha alterada com sucesso."
        ));
    }
}