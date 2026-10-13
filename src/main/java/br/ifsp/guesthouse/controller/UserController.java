
package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.UserRegistrationDTO;
import br.ifsp.guesthouse.dto.UserResponseDTO;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Usuarios", description = "Operações relacionadas aos usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationDTO dto) {
        UserAccount user = userService.register(dto);

        UserResponseDTO response = UserResponseDTO.fromEntity(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
