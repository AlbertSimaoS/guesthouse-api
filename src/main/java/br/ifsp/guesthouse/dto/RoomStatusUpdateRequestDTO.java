package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomStatusUpdateRequestDTO (
        @NotNull(message = "Room status is required")
        RoomStatus status
){
}
