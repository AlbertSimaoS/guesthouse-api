package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.RoomType;
import java.math.BigDecimal;

public record RoomTypeResponseDTO(
        Long id,
        String name,
        int capacity,
        BigDecimal dailyRate
) {
    public static RoomTypeResponseDTO fromEntity(RoomType roomType) {
        return new RoomTypeResponseDTO(
                roomType.getId(),
                roomType.getName(),
                roomType.getCapacity(),
                roomType.getDailyRate()
        );
    }
}