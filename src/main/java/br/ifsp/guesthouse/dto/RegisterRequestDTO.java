
package br.ifsp.guesthouse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "CPF is required")
        @Pattern(
                regexp = "\\d{11}",
                message = "CPF must contain exactly 11 digits"
        )
        String cpf,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Telephone is required")
        @Pattern(
                regexp = "^\\+?[1-9]\\d{1,14}$",
                message = "Telephone number must be in a valid international format"
        )
        String telephone,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 100,
                message = "Password must contain between 8 and 100 characters"
        )
        String password
) {}
