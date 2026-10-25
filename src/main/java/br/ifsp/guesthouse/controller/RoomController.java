package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.*;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.service.AvailabilityService;
import br.ifsp.guesthouse.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Rooms",
        description = "Endpoints for room management and availability"
)
@RestController
@RequestMapping("/room")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(
            summary = "Register a new room",
            description = "Registers a new room using the provided room details and returns the created room information."
    )
    @PostMapping("/register")
    public ResponseEntity<RoomResponseDTO> register(@Valid @RequestBody RoomRequestDTO dto){
        RoomResponseDTO response = roomService.register(dto);

        return  ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}/update-status")
    public ResponseEntity<String> updateRoomStatus(
            @PathVariable int id,
            @RequestBody @Valid RoomStatusUpdateRequestDTO request
    ) {
        roomService.updateRoomStatus(id, request.status());

        return ResponseEntity.ok("Status updated successfully.");
    }
}