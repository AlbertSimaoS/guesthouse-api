package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF01 (cadastro de hóspede) e RF02 (login com JWT). */
@DisplayName("RF01/RF02 - Autenticação")
class AuthFunctionalTest extends AbstractFunctionalTest {

    private static String registerJson(String cpf, String email, String password) {
        return """
                {"name":"Maria Silva","cpf":"%s","email":"%s","telephone":"+5511987654321","password":"%s"}
                """.formatted(cpf, email, password);
    }

    // ------------------------------------------------------------------ RF01

    @Test
    @DisplayName("RF01: cadastra hóspede, devolve 201 e nunca devolve a senha")
    void registerGuest() throws Exception {
        MvcResult result = doPost("/auth/register", null, registerJson("12345678901", "maria@email.com", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.profile").value("GUEST"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("RF01: a senha é gravada com hash, nunca em texto puro")
    void registerStoresPasswordHashed() throws Exception {
        doPost("/auth/register", null, registerJson("12345678901", "maria@email.com", PASSWORD))
                .andExpect(status().isCreated());

        UserAccount saved = userRepository.findByEmail("maria@email.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPassword())).isTrue();
        assertThat(saved.getProfile()).isEqualTo(Profile.GUEST);
    }

    @Test
    @DisplayName("RF01: e-mail duplicado retorna 409")
    void registerDuplicateEmail() throws Exception {
        doPost("/auth/register", null, registerJson("12345678901", "maria@email.com", PASSWORD))
                .andExpect(status().isCreated());

        doPost("/auth/register", null, registerJson("10987654321", "maria@email.com", PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RF01: CPF duplicado retorna 409")
    void registerDuplicateCpf() throws Exception {
        doPost("/auth/register", null, registerJson("12345678901", "maria@email.com", PASSWORD))
                .andExpect(status().isCreated());

        doPost("/auth/register", null, registerJson("12345678901", "outra@email.com", PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RF01: senha com menos de 8 caracteres é rejeitada")
    void registerPasswordTooShort() throws Exception {
        doPost("/auth/register", null, registerJson("12345678901", "maria@email.com", "1234567"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("RF01: senha com exatamente 8 caracteres é aceita")
    void registerPasswordMinimumLength() throws Exception {
        doPost("/auth/register", null, registerJson("12345678901", "maria@email.com", "12345678"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RF01: CPF fora do formato, e-mail inválido e campos ausentes retornam 400")
    void registerInvalidData() throws Exception {
        doPost("/auth/register", null, registerJson("123", "maria@email.com", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.cpf").exists());

        doPost("/auth/register", null, registerJson("12345678901", "nao-e-email", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        doPost("/auth/register", null, "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF01: JSON malformado retorna 400")
    void registerMalformedJson() throws Exception {
        doPost("/auth/register", null, "{isto nao e json")
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ RF02

    @Test
    @DisplayName("RF02: login válido devolve token JWT e os dados do usuário")
    void loginSuccess() throws Exception {
        createGuest("maria@email.com");

        MvcResult result = doPost(
                "/auth/login",
                null,
                "{\"email\":\"maria@email.com\",\"password\":\"%s\"}".formatted(PASSWORD)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("maria@email.com"))
                .andExpect(jsonPath("$.user.profile").value("GUEST"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        String token = read(result, "$.accessToken");
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("RF02: senha errada e e-mail inexistente dão a mesma resposta 401 (não revelam qual campo errou)")
    void loginInvalidCredentialsDoNotLeakWhichFieldIsWrong() throws Exception {
        createGuest("maria@email.com");

        MvcResult wrongPassword = doPost(
                "/auth/login", null,
                "{\"email\":\"maria@email.com\",\"password\":\"senha-errada-1\"}"
        ).andExpect(status().isUnauthorized()).andReturn();

        MvcResult unknownEmail = doPost(
                "/auth/login", null,
                "{\"email\":\"ninguem@email.com\",\"password\":\"%s\"}".formatted(PASSWORD)
        ).andExpect(status().isUnauthorized()).andReturn();

        String messageA = read(wrongPassword, "$.message");
        String messageB = read(unknownEmail, "$.message");
        assertThat(messageA).isEqualTo(messageB);
        assertThat(messageA.toLowerCase()).doesNotContain("senha não").doesNotContain("user not found");
    }

    @Test
    @DisplayName("RF02: login sem campos obrigatórios retorna 400")
    void loginValidation() throws Exception {
        doPost("/auth/login", null, "{\"email\":\"\",\"password\":\"\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF02: o token obtido no login dá acesso a rotas protegidas")
    void tokenGrantsAccess() throws Exception {
        String token = guestToken("maria@email.com");

        doGet("/reservation/my-reservations", token)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Segurança: rota protegida sem token retorna 401")
    void protectedRouteWithoutToken() throws Exception {
        doGet("/reservation/my-reservations", null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Segurança: token inválido retorna 401")
    void invalidToken() throws Exception {
        doGet("/reservation/my-reservations", "isto-nao-e-um-jwt")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Segurança: token de usuário que não existe mais retorna 401")
    void tokenOfDeletedUser() throws Exception {
        UserAccount user = createGuest("maria@email.com");
        String token = login("maria@email.com");
        userRepository.deleteById(user.getId());

        doGet("/reservation/my-reservations", token)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF02: o token identifica o perfil do usuário")
    void tokenCarriesProfile() throws Exception {
        String token = receptionToken();

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).containsIgnoringCase("RECEPTION");
    }

    // ----------------------------------------------- recuperação de senha

    @Test
    @DisplayName("Recuperação de senha: forgot + reset permitem logar com a nova senha")
    void passwordRecoveryFlow() throws Exception {
        UserAccount user = createGuest("maria@email.com");

        MvcResult forgot = doPost(
                "/auth/forgot-password", null,
                "{\"cpf\":\"%s\",\"email\":\"maria@email.com\"}".formatted(user.getCpf())
        ).andExpect(status().isOk()).andReturn();
        String resetToken = read(forgot, "$.token");

        doPost(
                "/auth/reset-password", null,
                "{\"token\":\"%s\",\"newPassword\":\"novaSenha123\",\"confirmPassword\":\"novaSenha123\"}"
                        .formatted(resetToken)
        ).andExpect(status().isOk());

        doPost("/auth/login", null, "{\"email\":\"maria@email.com\",\"password\":\"novaSenha123\"}")
                .andExpect(status().isOk());
        doPost("/auth/login", null, "{\"email\":\"maria@email.com\",\"password\":\"%s\"}".formatted(PASSWORD))
                .andExpect(status().isUnauthorized());

        // o token só vale uma vez
        doPost(
                "/auth/reset-password", null,
                "{\"token\":\"%s\",\"newPassword\":\"outraSenha123\",\"confirmPassword\":\"outraSenha123\"}"
                        .formatted(resetToken)
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Recuperação de senha: dados que não batem retornam 400 e confirmação diferente é rejeitada")
    void passwordRecoveryErrors() throws Exception {
        UserAccount user = createGuest("maria@email.com");

        doPost("/auth/forgot-password", null, "{\"cpf\":\"99999999999\",\"email\":\"maria@email.com\"}")
                .andExpect(status().isBadRequest());

        MvcResult forgot = doPost(
                "/auth/forgot-password", null,
                "{\"cpf\":\"%s\",\"email\":\"maria@email.com\"}".formatted(user.getCpf())
        ).andExpect(status().isOk()).andReturn();
        String resetToken = read(forgot, "$.token");

        doPost(
                "/auth/reset-password", null,
                "{\"token\":\"%s\",\"newPassword\":\"novaSenha123\",\"confirmPassword\":\"diferente123\"}"
                        .formatted(resetToken)
        ).andExpect(status().isBadRequest());

        doPost(
                "/auth/reset-password", null,
                "{\"token\":\"token-inexistente\",\"newPassword\":\"novaSenha123\",\"confirmPassword\":\"novaSenha123\"}"
        ).andExpect(status().isBadRequest());
    }
}
