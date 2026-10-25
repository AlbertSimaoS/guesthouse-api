package br.ifsp.guesthouse.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras de validação dos DTOs de entrada (Bean Validation). */
@DisplayName("Validação dos DTOs (unitário)")
class DtoValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> invalidFields(Object dto) {
        return VALIDATOR.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static RegisterRequestDTO register(String cpf, String email, String phone, String password) {
        return new RegisterRequestDTO("Maria", cpf, email, phone, password);
    }

    @Test
    @DisplayName("RF01: cadastro válido não tem violações")
    void validRegistration() {
        assertThat(invalidFields(register("12345678901", "m@e.com", "+5511987654321", "12345678"))).isEmpty();
    }

    @Test
    @DisplayName("RF01: senha com menos de 8 caracteres é inválida (7 não, 8 sim, 101 não)")
    void passwordLength() {
        assertThat(invalidFields(register("12345678901", "m@e.com", "+5511987654321", "1234567"))).contains("password");
        assertThat(invalidFields(register("12345678901", "m@e.com", "+5511987654321", "12345678"))).doesNotContain("password");
        assertThat(invalidFields(register("12345678901", "m@e.com", "+5511987654321", "x".repeat(101)))).contains("password");
    }

    @Test
    @DisplayName("RF01: CPF, e-mail e telefone com formato inválido")
    void registrationFormats() {
        assertThat(invalidFields(register("123", "m@e.com", "+5511987654321", "12345678"))).contains("cpf");
        assertThat(invalidFields(register("1234567890a", "m@e.com", "+5511987654321", "12345678"))).contains("cpf");
        assertThat(invalidFields(register("12345678901", "sem-arroba", "+5511987654321", "12345678"))).contains("email");
        assertThat(invalidFields(register("12345678901", "m@e.com", "telefone", "12345678"))).contains("telephone");
        assertThat(invalidFields(new RegisterRequestDTO(null, null, null, null, null)))
                .contains("name", "cpf", "email", "telephone", "password");
    }

    @Test
    @DisplayName("RF02: login exige e-mail válido e senha")
    void loginValidation() {
        assertThat(invalidFields(new LoginRequestDTO("m@e.com", "x"))).isEmpty();
        assertThat(invalidFields(new LoginRequestDTO("", ""))).contains("email", "password");
        assertThat(invalidFields(new LoginRequestDTO("invalido", "x"))).contains("email");
    }

    @Test
    @DisplayName("RF03: tipo de quarto exige nome, capacidade >= 1 e diária > 0")
    void roomTypeValidation() {
        assertThat(invalidFields(new RoomTypeRequestDTO("Duplo", 2, new BigDecimal("100")))).isEmpty();
        assertThat(invalidFields(new RoomTypeRequestDTO("", 2, new BigDecimal("100")))).contains("name");
        assertThat(invalidFields(new RoomTypeRequestDTO("Duplo", 0, new BigDecimal("100")))).contains("capacity");
        assertThat(invalidFields(new RoomTypeRequestDTO("Duplo", 2, BigDecimal.ZERO))).contains("dailyRate");
        assertThat(invalidFields(new RoomTypeRequestDTO("Duplo", 2, null))).contains("dailyRate");
    }

    @Test
    @DisplayName("RF03: quarto exige número de até 3 caracteres e tipo positivo")
    void roomValidation() {
        assertThat(invalidFields(new RoomRequestDTO("101", 1))).isEmpty();
        assertThat(invalidFields(new RoomRequestDTO("", 1))).contains("number");
        assertThat(invalidFields(new RoomRequestDTO("1234", 1))).contains("number");
        assertThat(invalidFields(new RoomRequestDTO("101", null))).contains("typeId");
        assertThat(invalidFields(new RoomRequestDTO("101", 0))).contains("typeId");
    }

    @Test
    @DisplayName("RF04: temporada exige nome, datas e multiplicador > 0")
    void seasonValidation() {
        LocalDate s = LocalDate.of(2026, 12, 1);
        assertThat(invalidFields(new SeasonRequestDTO("Alta", s, s.plusDays(5), new BigDecimal("1.5")))).isEmpty();
        assertThat(invalidFields(new SeasonRequestDTO("", s, s.plusDays(5), new BigDecimal("1.5")))).contains("name");
        assertThat(invalidFields(new SeasonRequestDTO("Alta", null, null, new BigDecimal("1.5"))))
                .contains("startDate", "endDate");
        assertThat(invalidFields(new SeasonRequestDTO("Alta", s, s, BigDecimal.ZERO))).contains("multiplier");
    }

    @Test
    @DisplayName("RF05: consulta de disponibilidade exige datas futuras e hóspedes > 0")
    void availabilityValidation() {
        LocalDate future = LocalDate.now().plusDays(5);
        assertThat(invalidFields(new AvailabilityRequestDTO(future, future.plusDays(1), 2))).isEmpty();
        assertThat(invalidFields(new AvailabilityRequestDTO(LocalDate.now().minusDays(1), future, 2))).contains("startDate");
        assertThat(invalidFields(new AvailabilityRequestDTO(future, future.plusDays(1), 0))).contains("numberOfGuests");
        assertThat(invalidFields(new AvailabilityRequestDTO(null, null, null)))
                .contains("startDate", "endDate", "numberOfGuests");
    }

    @Test
    @DisplayName("RF06: reserva exige tipo, datas futuras e hóspedes > 0")
    void reservationValidation() {
        LocalDate future = LocalDate.now().plusDays(5);
        assertThat(invalidFields(new ReservationRequestDTO(1, future, future.plusDays(2), 2))).isEmpty();
        assertThat(invalidFields(new ReservationRequestDTO(null, future, future.plusDays(2), 2))).contains("roomTypeId");
        assertThat(invalidFields(new ReservationRequestDTO(0, future, future.plusDays(2), 2))).contains("roomTypeId");
        assertThat(invalidFields(new ReservationRequestDTO(1, LocalDate.now().minusDays(1), future, 2))).contains("startDate");
        assertThat(invalidFields(new ReservationRequestDTO(1, future, future.plusDays(2), -1))).contains("numberOfGuests");
    }

    @Test
    @DisplayName("Redefinição de senha e status exigem os campos")
    void otherDtos() {
        assertThat(invalidFields(new ResetPasswordRequestDTO("t", "12345678", "12345678"))).isEmpty();
        assertThat(invalidFields(new ResetPasswordRequestDTO("", "123", ""))).contains("token", "newPassword", "confirmPassword");
        assertThat(invalidFields(new ForgotPasswordRequestDTO("123", "x"))).contains("cpf", "email");
        assertThat(invalidFields(new PaymentUpdateRequestDTO(null))).contains("status");
        assertThat(invalidFields(new RoomStatusUpdateRequestDTO(null))).contains("status");
        assertThat(invalidFields(new OccupancyReportRequestDTO(null, null))).contains("startDate", "endDate");
    }
}
