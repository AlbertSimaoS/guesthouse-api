package br.ifsp.guesthouse.dto;

import java.math.BigDecimal;

public record RoomAvailabilityResponseDTO(
        Long roomTypeId,
        String roomTypeName,
        Integer capacity,
        Long availableRooms,
        BigDecimal nightlyRate,
        BigDecimal totalStayPrice
) {
}