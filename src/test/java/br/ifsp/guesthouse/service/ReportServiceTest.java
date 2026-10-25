package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.DailyMovementReportResponseDTO;
import br.ifsp.guesthouse.dto.OccupancyReportResponseDTO;
import br.ifsp.guesthouse.dto.RevenueReportResponseDTO;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReportService (unitário) - RF14/RF15/RF16")
class ReportServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @InjectMocks private ReportService service;

    private final LocalDate d0 = LocalDate.of(2026, 12, 1);
    private final UserAccount guest = TestData.user(1L, "maria@email.com", Profile.GUEST);
    private final RoomType standard = TestData.roomType(1, "Standard", 2, "100.00");

    private Reservation res(long id, LocalDate s, LocalDate e, String total, ReservationStatus st) {
        return TestData.reservation(id, guest, standard, s, e, total, st);
    }

    @Test
    @DisplayName("ocupação: noites ocupadas / (quartos x dias)")
    void occupancyRate() {
        when(roomRepository.count()).thenReturn(2L);
        when(reservationRepository.findActiveReservationsInPeriod(any(), any(), any()))
                .thenReturn(List.of(res(1, d0.plusDays(1), d0.plusDays(3), "200", ReservationStatus.CONFIRMED)));

        OccupancyReportResponseDTO report = service.getOccupancyRate(d0, d0.plusDays(4));

        assertThat(report.totalRooms()).isEqualTo(2);
        assertThat(report.occupiedRooms()).isEqualTo(2);
        assertThat(report.occupancyRatePercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("ocupação: reservas que ultrapassam o período são recortadas")
    void occupancyClipsToPeriod() {
        when(roomRepository.count()).thenReturn(1L);
        when(reservationRepository.findActiveReservationsInPeriod(any(), any(), any()))
                .thenReturn(List.of(res(1, d0.minusDays(5), d0.plusDays(20), "999", ReservationStatus.CHECKED_IN)));

        OccupancyReportResponseDTO report = service.getOccupancyRate(d0, d0.plusDays(2));

        assertThat(report.occupiedRooms()).isEqualTo(2);
        assertThat(report.occupancyRatePercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("ocupação: sem quartos devolve zero; período invertido é inválido; período de 1 dia conta 1")
    void occupancyEdges() {
        when(roomRepository.count()).thenReturn(0L);
        assertThat(service.getOccupancyRate(d0, d0.plusDays(3)).occupancyRatePercentage()).isEqualByComparingTo("0");

        assertThatThrownBy(() -> service.getOccupancyRate(d0.plusDays(3), d0))
                .isInstanceOf(IllegalArgumentException.class);

        when(roomRepository.count()).thenReturn(1L);
        when(reservationRepository.findActiveReservationsInPeriod(any(), any(), any())).thenReturn(List.of());
        assertThat(service.getOccupancyRate(d0, d0).occupancyRatePercentage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("receita: soma por tipo de quarto; diárias separadas de consumos")
    void revenueByType() {
        RoomType suite = TestData.roomType(2, "Suite", 3, "400.00");
        when(roomTypeRepository.findAll()).thenReturn(List.of(standard, suite));
        when(reservationRepository.findByRoomTypeIdAndPeriod(eq(1), any(), any(), any())).thenReturn(List.of(
                res(1, d0, d0.plusDays(2), "200.00", ReservationStatus.CONFIRMED),
                res(2, d0, d0.plusDays(3), "300.00", ReservationStatus.FINISHED)));
        when(reservationRepository.findByRoomTypeIdAndPeriod(eq(2), any(), any(), any())).thenReturn(List.of());

        List<RevenueReportResponseDTO> report = service.getRevenueByRoomType(d0, d0.plusDays(10));

        assertThat(report).hasSize(2);
        assertThat(report.get(0).roomTypeName()).isEqualTo("Standard");
        assertThat(report.get(0).totalDailyRates()).isEqualByComparingTo("500.00");
        assertThat(report.get(0).totalRevenue()).isEqualByComparingTo("500.00");
        assertThat(report.get(1).totalRevenue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("receita: período invertido é inválido")
    void revenueInvalidPeriod() {
        assertThatThrownBy(() -> service.getRevenueByRoomType(d0.plusDays(3), d0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("movimento do dia: só reservas válidas entram nas entradas e saídas")
    void dailyMovements() {
        when(reservationRepository.findByStartDate(d0)).thenReturn(List.of(
                res(1, d0, d0.plusDays(2), "200", ReservationStatus.CONFIRMED),
                res(2, d0, d0.plusDays(2), "200", ReservationStatus.CANCELLED)));
        when(reservationRepository.findByEndDate(d0)).thenReturn(List.of(
                res(3, d0.minusDays(2), d0, "200", ReservationStatus.CHECKED_IN)));

        DailyMovementReportResponseDTO report = service.getDailyMovements(d0);

        assertThat(report.targetDate()).isEqualTo(d0);
        assertThat(report.totalExpectedCheckIns()).isEqualTo(1);
        assertThat(report.totalExpectedCheckOuts()).isEqualTo(1);
        assertThat(report.expectedCheckIns().get(0).guestName()).isEqualTo(guest.getName());
        assertThat(report.expectedCheckIns().get(0).roomTypeName()).isEqualTo("Standard");
    }
}
