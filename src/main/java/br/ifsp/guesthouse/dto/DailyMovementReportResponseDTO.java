package br.ifsp.guesthouse.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyMovementReportResponseDTO(
        LocalDate targetDate,
        long totalExpectedCheckIns,
        long totalExpectedCheckOuts,
        List<ReservationMovementDTO> expectedCheckIns,
        List<ReservationMovementDTO> expectedCheckOuts
) {
}