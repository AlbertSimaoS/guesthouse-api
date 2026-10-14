package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.RoomRequestDTO;
import br.ifsp.guesthouse.dto.RoomResponseDTO;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.service.RoomService;
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
@RequestMapping("/room")
@Tag(name = "Quartos", description = "Operações relacionadas aos usuarios")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/register")
    public ResponseEntity<RoomResponseDTO> register(@Valid @RequestBody RoomRequestDTO dto){
        RoomResponseDTO response = roomService.register(dto);

        return  ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
