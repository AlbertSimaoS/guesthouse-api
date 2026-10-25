package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.DailyMovementReportResponseDTO;
import br.ifsp.guesthouse.dto.OccupancyReportResponseDTO;
import br.ifsp.guesthouse.dto.ReservationMovementDTO;
import br.ifsp.guesthouse.dto.RevenueReportResponseDTO;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private static final List<ReservationStatus> VALID_STATUSES = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.FINISHED
    );

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public ReportService(ReservationRepository reservationRepository,
                         RoomRepository roomRepository,
                         RoomTypeRepository roomTypeRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public OccupancyReportResponseDTO getOccupancyRate(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        long totalRooms = roomRepository.count();
        if (totalRooms == 0) {
            return new OccupancyReportResponseDTO(startDate, endDate, 0, 0, BigDecimal.ZERO);
        }

        long totalDays = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
        long totalAvailableRoomNights = totalRooms * totalDays;

        List<Reservation> overlapping = reservationRepository
                .findActiveReservationsInPeriod(startDate, endDate, VALID_STATUSES);

        long occupiedRoomNights = 0;
        for (Reservation reservation : overlapping) {
            LocalDate effectiveStart = reservation.getStartDate().isBefore(startDate)
                    ? startDate : reservation.getStartDate();
            LocalDate effectiveEnd = reservation.getEndDate().isAfter(endDate)
                    ? endDate : reservation.getEndDate();

            long nights = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd);
            if (nights > 0) {
                occupiedRoomNights += nights;
            }
        }

        BigDecimal occupancyRate = BigDecimal.valueOf(occupiedRoomNights)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalAvailableRoomNights), 2, RoundingMode.HALF_UP);

        return new OccupancyReportResponseDTO(
                startDate, endDate, totalRooms, occupiedRoomNights, occupancyRate);
    }

    public List<RevenueReportResponseDTO> getRevenueByRoomType(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        List<RoomType> roomTypes = roomTypeRepository.findAll();
        List<RevenueReportResponseDTO> reportList = new ArrayList<>();

        for (RoomType roomType : roomTypes) {
            List<Reservation> reservations = reservationRepository.findByRoomTypeIdAndPeriod(
                    roomType.getId(), startDate, endDate, VALID_STATUSES);

            BigDecimal totalDailyRates = BigDecimal.ZERO;
            BigDecimal totalConsumptions = BigDecimal.ZERO;

            for (Reservation reservation : reservations) {
                if (reservation.getTotalPrice() != null) {
                    totalDailyRates = totalDailyRates.add(reservation.getTotalPrice());
                }
            }

            reportList.add(new RevenueReportResponseDTO(
                    roomType.getId(),
                    roomType.getName(),
                    startDate,
                    endDate,
                    totalDailyRates,
                    totalConsumptions,
                    totalDailyRates.add(totalConsumptions)
            ));
        }

        return reportList;
    }

    @Transactional(readOnly = true)
    public DailyMovementReportResponseDTO getDailyMovements(LocalDate date) {

        List<ReservationMovementDTO> checkIns = reservationRepository.findByStartDate(date).stream()
                .filter(r -> VALID_STATUSES.contains(r.getStatus()))
                .map(ReservationMovementDTO::new)
                .toList();

        List<ReservationMovementDTO> checkOuts = reservationRepository.findByEndDate(date).stream()
                .filter(r -> VALID_STATUSES.contains(r.getStatus()))
                .map(ReservationMovementDTO::new)
                .toList();

        return new DailyMovementReportResponseDTO(
                date, checkIns.size(), checkOuts.size(), checkIns, checkOuts);
    }
}