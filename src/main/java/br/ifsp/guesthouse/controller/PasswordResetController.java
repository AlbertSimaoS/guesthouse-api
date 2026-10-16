package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.ForgotPasswordRequestDTO;
import br.ifsp.guesthouse.dto.PasswordResetTokenResponseDTO;
import br.ifsp.guesthouse.dto.ResetPasswordRequestDTO;
import br.ifsp.guesthouse.service.PasswordResetService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetTokenResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        PasswordResetTokenResponseDTO response = passwordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(response);
    }

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