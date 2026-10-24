package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.RoomType;
import java.math.BigDecimal;

public record AvailabilityResponseDTO(
        Integer roomTypeId,
        String roomTypeName,
        Integer capacity,
        Integer availableRooms,
        BigDecimal nightlyRate,
        BigDecimal totalStayPrice
) {
    public static AvailabilityResponseDTO from(
            RoomType roomType,
            int availableRooms,
            BigDecimal totalStayPrice
    ) {
        return new AvailabilityResponseDTO(
                roomType.getId(),
                roomType.getName(),
                roomType.getCapacity(),
                availableRooms,
                roomType.getDailyRate(),
                totalStayPrice
        );
    }
}