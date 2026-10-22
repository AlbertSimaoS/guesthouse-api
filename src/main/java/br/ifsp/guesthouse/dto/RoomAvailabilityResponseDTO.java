package br.ifsp.guesthouse.dto;

import java.math.BigDecimal;

public record RoomAvailabilityResponseDTO(
        Integer roomTypeId,
        String roomTypeName,
        Integer capacity,
        Integer availableRooms,
        BigDecimal nightlyRate,
        BigDecimal totalStayPrice
) {
}