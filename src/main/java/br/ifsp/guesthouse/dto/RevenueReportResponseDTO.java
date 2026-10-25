package br.ifsp.guesthouse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueReportResponseDTO(
        Integer roomTypeId,
        String roomTypeName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDailyRates,
        BigDecimal totalConsumptions,
        BigDecimal totalRevenue
) {
}