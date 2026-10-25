package br.ifsp.guesthouse.config;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.exception.RestAuthenticationEntryPoint;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import br.ifsp.guesthouse.service.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter (unitário)")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserAccountRepository userRepository;
    @Mock private RestAuthenticationEntryPoint entryPoint;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userRepository, entryPoint);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sem cabeçalho Authorization segue adiante sem autenticar")
    void noHeader() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("cabeçalho que não é Bearer é ignorado")
    void nonBearerHeader() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("token válido autentica com o perfil do usuário como ROLE_*")
    void validToken() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtService.extractUserId("good")).thenReturn(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(TestData.user(9L, "r@p.com", Profile.RECEPTION)));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(9L);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RECEPTION");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("token inválido gera 401 via entry point e interrompe a cadeia")
    void invalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer bad");
        when(jwtService.extractUserId("bad")).thenThrow(new MalformedJwtException("bad"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("token de usuário que não existe mais gera 401")
    void unknownUser() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtService.extractUserId("good")).thenReturn(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("id inválido no token (IllegalArgumentException) gera 401")
    void illegalArgument() throws Exception {
        request.addHeader("Authorization", "Bearer weird");
        when(jwtService.extractUserId("weird")).thenThrow(new IllegalArgumentException("x"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(any(), any(), any(AuthenticationException.class));
        verify(userRepository, never()).findById(any());
    }
}
