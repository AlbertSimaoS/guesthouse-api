package br.ifsp.guesthouse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OccupancyReportResponseDTO(
        LocalDate startDate,
        LocalDate endDate,
        long totalRooms,
        long occupiedRooms,
        BigDecimal occupancyRatePercentage
) {
}