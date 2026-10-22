package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.enums.RoomStatus;

public record RoomResponseDTO(
        Integer id,
        String number,
        RoomStatus status,
        Integer typeId,
        String typeName
) {
    public RoomResponseDTO(Room room) {
        this(
                room.getId(),
                room.getNumber(),
                room.getStatus(),
                room.getType().getId(),
                room.getType().getName()
        );
    }
}