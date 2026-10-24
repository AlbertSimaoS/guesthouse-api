package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.*;
import br.ifsp.guesthouse.service.AvailabilityService;
import br.ifsp.guesthouse.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Room types",
        description = "Endpoints for room type management."
)
@RestController
@RequestMapping("/room-types")
public class RoomTypeController {
    private final RoomTypeService roomTypeService;
    private final AvailabilityService availabilityService;

    public RoomTypeController(RoomTypeService roomTypeService, AvailabilityService availabilityService) {
        this.roomTypeService = roomTypeService;
        this.availabilityService = availabilityService;
    }

    @Operation(
            summary = "Register a new room type",
            description = "Registers a new room type with the provided details, such as its name, capacity, and daily rate, and returns the created room type information."
    )
    @PostMapping("/register")
    public ResponseEntity<RoomTypeResponseDTO> register(@Valid @RequestBody RoomTypeRequestDTO dto) {
        RoomTypeResponseDTO response = roomTypeService.register(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Find available rooms",
            description = "Retrieves a paginated list of available rooms based on the requested check-in date, check-out date, and number of guests."
    )
    @GetMapping("/availability")
    public ResponseEntity<PageResponseDTO<AvailabilityResponseDTO>>
    availableRoomTypes(
            @Valid @ModelAttribute AvailabilityRequestDTO request,
            Pageable pageable
    ) {
        List<AvailabilityResponseDTO> availableRoomTypes =
                availabilityService.availableRoomTypes(request);

        PageResponseDTO<AvailabilityResponseDTO> response =
                availabilityService.paginateAvailableRoomTypes(
                        availableRoomTypes,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}