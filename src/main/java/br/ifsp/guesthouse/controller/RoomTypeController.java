package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.RoomTypeRequestDTO;
import br.ifsp.guesthouse.dto.RoomTypeResponseDTO;
import br.ifsp.guesthouse.service.RoomTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room-types")
@Tag(name = "Tipos de quarto", description = "Operações relacionadas aos tipos de quarto")
public class RoomTypeController {
    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @PostMapping("/register")
    public ResponseEntity<RoomTypeResponseDTO> register(@Valid @RequestBody RoomTypeRequestDTO dto) {
        RoomTypeResponseDTO response = roomTypeService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
