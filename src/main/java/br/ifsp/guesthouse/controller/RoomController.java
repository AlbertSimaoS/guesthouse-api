package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.*;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.service.RoomService;
import br.ifsp.guesthouse.service.RoomTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/availability")
    public ResponseEntity<PageResponseDTO<RoomAvailabilityResponseDTO>> findAvailableRooms(
            @Valid @ModelAttribute RoomAvailabilityRequestDTO request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                roomService.findAvailableRooms(request, pageable)
        );
    }
}
