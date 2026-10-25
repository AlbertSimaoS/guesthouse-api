package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.model.enums.Profile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService (unitário)")
class JwtServiceTest {

    private static final String SECRET = "VGhpc0lzQVNlY3JldEtleUZvckpXVFRlc3RzMTIzNDU2Nzg=";
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString("AnotherSecretKeyForJwtTests-1234567890".getBytes(StandardCharsets.UTF_8));
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtService service = new JwtService(SECRET, ONE_HOUR);

    @Test
    @DisplayName("gera um token JWT (3 partes) e extrai o id do usuário de volta")
    void generateAndExtract() {
        String token = service.generateToken(42L, Profile.GUEST);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(service.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("o token traz emissão e expiração coerentes com a configuração")
    void tokenHasExpiration() {
        String token = service.generateToken(7L, Profile.GUEST);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isBetween(ONE_HOUR - 2_000, ONE_HOUR + 2_000);
    }

    @Test
    @DisplayName("token expirado é rejeitado")
    void expiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1);
        String token = shortLived.generateToken(1L, Profile.GUEST);
        Thread.sleep(50);

        assertThatThrownBy(() -> shortLived.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token assinado com outra chave é rejeitado")
    void tokenSignedWithAnotherKey() {
        String forged = new JwtService(OTHER_SECRET, ONE_HOUR).generateToken(1L, Profile.GUEST);

        assertThatThrownBy(() -> service.extractUserId(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("token adulterado ou lixo é rejeitado")
    void tamperedOrGarbageToken() {
        String token = service.generateToken(1L, Profile.GUEST);
        String[] parts = token.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"999\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> service.extractUserId(tampered)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.extractUserId("lixo")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("expiração menor ou igual a zero é inválida")
    void invalidExpiration() {
        assertThatThrownBy(() -> new JwtService(SECRET, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(SECRET, -5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("o token identifica usuário e perfil (claim 'role')")
    void tokenCarriesProfile() {
        String token = service.generateToken(5L, Profile.RECEPTION);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("5");
        assertThat(claims.get("role", String.class)).isEqualTo("RECEPTION");
        assertThat(service.extractUserId(token)).isEqualTo(5L);
    }
}