package br.ifsp.guesthouse.exception;

import br.ifsp.guesthouse.dto.ApiErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler (unitário)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertResponse(ResponseEntity<ApiErrorResponse> response, int status, String message) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status);
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("recurso duplicado -> 409")
    void duplicate() {
        assertResponse(handler.handleDuplicateResource(new DuplicateResourceException("dup")), 409, "dup");
    }

    @Test
    @DisplayName("não encontrado -> 404")
    void notFound() {
        assertResponse(handler.handleResourceNotFound(new ResourceNotFoundException("nf")), 404, "nf");
    }

    @Test
    @DisplayName("argumento inválido -> 400")
    void illegalArgument() {
        assertResponse(handler.handleIllegalArgument(new IllegalArgumentException("ruim")), 400, "ruim");
    }

    @Test
    @DisplayName("credenciais inválidas -> 401 com mensagem genérica")
    void badCredentials() {
        assertResponse(handler.handleBadCredentials(new BadCredentialsException("qualquer")), 401,
                "Invalid email or password");
    }

    @Test
    @DisplayName("NullPointerException -> 500")
    void nullPointer() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNullPointerException(new NullPointerException());
        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    @DisplayName("BusinessException carrega a mensagem")
    void businessException() {
        assertThat(new BusinessException("regra").getMessage()).isEqualTo("regra");
    }
}
