package br.ifsp.guesthouse.controller;

import br.ifsp.guesthouse.dto.*;
import br.ifsp.guesthouse.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/occupancy")
    public ResponseEntity<OccupancyReportResponseDTO> getOccupancyReport(
            @Valid OccupancyReportRequestDTO request
    ) {

        OccupancyReportResponseDTO report = reportService.getOccupancyRate(
                request.startDate(),
                request.endDate()
        );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueReportResponseDTO>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<RevenueReportResponseDTO> report = reportService.getRevenueByRoomType(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/daily-movements")
    public ResponseEntity<DailyMovementReportResponseDTO> getDailyMovementsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // Se a data não for informada, assume o dia de hoje
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        DailyMovementReportResponseDTO report = reportService.getDailyMovements(targetDate);
        return ResponseEntity.ok(report);
    }
}