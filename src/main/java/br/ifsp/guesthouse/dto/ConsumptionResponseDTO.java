package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.Consumption;
import br.ifsp.guesthouse.model.enums.ConsumptionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumptionResponseDTO(
        Long id,
        Long reservationId,
        ConsumptionType type,
        String description,
        BigDecimal amount,
        LocalDateTime createdAt
) {
    public static ConsumptionResponseDTO from(
            Consumption consumption
    ) {
        return new ConsumptionResponseDTO(
                consumption.getId(),
                consumption.getReservation().getId(),
                consumption.getType(),
                consumption.getDescription(),
                consumption.getAmount(),
                consumption.getCreatedAt()
        );
    }
}