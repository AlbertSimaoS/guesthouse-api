
package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.LoginRequestDTO;
import br.ifsp.guesthouse.dto.LoginResponseDTO;
import br.ifsp.guesthouse.dto.UserRequestDTO;
import br.ifsp.guesthouse.dto.UserResponseDTO;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.service.AuthService;
import br.ifsp.guesthouse.service.JwtService;
import br.ifsp.guesthouse.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Operações relacionadas a autenticação dos usuários")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthService authService, JwtService jwtService, UserService userService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        UserAccount user = userService.register(dto);

        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {

        UserAccount user = authService.authenticate(
                request.email(),
                request.password()
        );

        String token = jwtService.generateToken(user.getId());

        UserResponseDTO userResponse =
                UserResponseDTO.fromEntity(user);

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                "Bearer",
                userResponse
        );

        return ResponseEntity.ok(response);
    }
}
