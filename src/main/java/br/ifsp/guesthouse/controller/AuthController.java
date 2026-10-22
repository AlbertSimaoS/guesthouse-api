
package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.LoginRequestDTO;
import br.ifsp.guesthouse.dto.LoginResponseDTO;
import br.ifsp.guesthouse.dto.RegisterRequestDTO;
import br.ifsp.guesthouse.dto.RegisterResponseDTO;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.service.AuthService;
import br.ifsp.guesthouse.service.JwtService;
import br.ifsp.guesthouse.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Authentication",
        description = "Endpoints for registration, login and password management"
)
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthService authService, JwtService jwtService, UserService userService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new guest account and returns the registered user's information."
    )
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO dto
    ) {

        UserAccount user = userService.register(dto);

        RegisterResponseDTO response = RegisterResponseDTO.fromEntity(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Authenticate a user",
            description = "Authenticates a user using their email and password. If the credentials are valid, returns a JWT access token, its type (Bearer), and the user's information."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        UserAccount user = authService.authenticate(
                request.email(),
                request.password()
        );

        String token = jwtService.generateToken(user.getId());

        RegisterResponseDTO response = RegisterResponseDTO.fromEntity(user);

        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(
                token,
                "Bearer",
                response
        );

        return ResponseEntity.ok(loginResponseDTO);
    }
}
