package br.ifsp.guesthouse.dto;

public record PasswordResetTokenResponseDTO(
        String message,
        String token
) {}