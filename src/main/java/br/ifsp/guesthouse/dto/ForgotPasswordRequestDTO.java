package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequestDTO(

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(
                regexp = "\\d{11}",
                message = "O CPF deve conter 11 dígitos númericos"
        )
        String cpf,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email
) {}