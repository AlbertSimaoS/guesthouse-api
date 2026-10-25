package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF14 (ocupação), RF15 (receita por tipo) e RF16 (entradas e saídas do dia). */
@DisplayName("RF14/RF15/RF16 - Relatórios da recepção")
class ReportFunctionalTest extends AbstractFunctionalTest {

    private static String period(String path, Object start, Object end) {
        return "%s?startDate=%s&endDate=%s".formatted(path, start, end);
    }

    // ------------------------------------------------------------------ RF14

    @Test
    @DisplayName("RF14: taxa de ocupação = noites ocupadas / (quartos x dias do período)")
    void occupancyRate() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createRoom("102", standard, RoomStatus.AVAILABLE);
        // 2 noites ocupadas dentro do período de 4 dias com 2 quartos: 2 / 8 = 25%
        createReservation(guest, standard, day(1), day(3), 2, "200.00", ReservationStatus.CONFIRMED);
        // não contam: pendente, cancelada e expirada
        createReservation(guest, standard, day(1), day(3), 2, "200.00", ReservationStatus.PENDING);
        createReservation(guest, standard, day(1), day(3), 2, "200.00", ReservationStatus.CANCELLED);
        createReservation(guest, standard, day(1), day(3), 2, "200.00", ReservationStatus.EXPIRED);

        MvcResult result = doGet(period("/report/occupancy", day(0), day(4)), receptionToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(2))
                .andExpect(jsonPath("$.occupiedRooms").value(2))
                .andReturn();

        assertThat(number(result, "$.occupancyRatePercentage")).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("RF14: só conta as noites que caem dentro do período consultado")
    void occupancyClipsReservationsToThePeriod() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        // reserva de 10 noites; o período consultado pega só 2 delas
        createReservation(guest, standard, day(0), day(10), 2, "1000.00", ReservationStatus.CHECKED_IN);

        MvcResult result = doGet(period("/report/occupancy", day(4), day(6)), receptionToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedRooms").value(2))
                .andReturn();

        // 2 noites / (1 quarto x 2 dias) = 100%
        assertThat(number(result, "$.occupancyRatePercentage")).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("RF14: sem quartos cadastrados a taxa é 0%")
    void occupancyWithoutRooms() throws Exception {
        MvcResult result = doGet(period("/report/occupancy", day(0), day(4)), receptionToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(0))
                .andReturn();

        assertThat(number(result, "$.occupancyRatePercentage")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("RF14: período invertido ou sem datas retorna 400")
    void occupancyValidation() throws Exception {
        String reception = receptionToken();

        doGet(period("/report/occupancy", day(10), day(1)), reception).andExpect(status().isBadRequest());
        doGet("/report/occupancy", reception).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ RF15

    @Test
    @DisplayName("RF15: receita por tipo de quarto, separando diárias e consumos")
    void revenueByRoomType() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        RoomType suite = createRoomType("Suite", 3, "400.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createRoom("201", suite, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(1), day(3), 2, "200.00", ReservationStatus.CONFIRMED);
        createReservation(guest, standard, day(5), day(8), 2, "300.00", ReservationStatus.FINISHED);
        createReservation(guest, standard, day(2), day(4), 2, "999.00", ReservationStatus.CANCELLED);
        createReservation(guest, standard, day(2), day(4), 2, "999.00", ReservationStatus.PENDING);
        createReservation(guest, suite, day(1), day(2), 2, "400.00", ReservationStatus.CHECKED_IN);

        MvcResult result = doGet(period("/report/revenue", day(0), day(10)), receptionToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].totalConsumptions").exists())
                .andReturn();

        List<Number> standardDailyRates = read(result, "$[?(@.roomTypeName=='Standard')].totalDailyRates");
        List<Number> suiteDailyRates = read(result, "$[?(@.roomTypeName=='Suite')].totalDailyRates");
        List<Number> standardTotal = read(result, "$[?(@.roomTypeName=='Standard')].totalRevenue");

        assertThat(standardDailyRates.get(0).doubleValue()).isEqualTo(500.00);
        assertThat(suiteDailyRates.get(0).doubleValue()).isEqualTo(400.00);
        assertThat(standardTotal.get(0).doubleValue()).isEqualTo(500.00);
    }

    @Test
    @DisplayName("RF15: reservas fora do período não entram na receita")
    void revenueIgnoresReservationsOutsideThePeriod() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(30), day(32), 2, "200.00", ReservationStatus.CONFIRMED);

        MvcResult result = doGet(period("/report/revenue", day(0), day(10)), receptionToken())
                .andExpect(status().isOk())
                .andReturn();

        List<Number> totals = read(result, "$[?(@.roomTypeName=='Standard')].totalRevenue");
        assertThat(totals.get(0).doubleValue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RF15: período invertido retorna 400")
    void revenueValidation() throws Exception {
        doGet(period("/report/revenue", day(10), day(1)), receptionToken()).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ RF16

    @Test
    @DisplayName("RF16: dia sem movimento devolve zeros; sem a data, assume hoje")
    void dailyMovementsEmptyDay() throws Exception {
        String reception = receptionToken();

        doGet("/report/daily-movements?date=" + day(5), reception)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpectedCheckIns").value(0))
                .andExpect(jsonPath("$.totalExpectedCheckOuts").value(0));

        doGet("/report/daily-movements", reception)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetDate").value(day(0).toString()));
    }

    @Test
    @DisplayName("RF16: lista as entradas e saídas previstas do dia")
    @Disabled("BUG: ReportService.getDailyMovements não é @Transactional e ReservationMovementDTO acessa "
            + "reservation.getRoomType()/getGuest() (LAZY) fora da sessão -> LazyInitializationException (500). "
            + "Com open-in-view=false isso quebra sempre que há reservas no dia.")
    void dailyMovements() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(5), day(7), 2, "200.00", ReservationStatus.CONFIRMED);
        createReservation(guest, standard, day(3), day(5), 1, "200.00", ReservationStatus.CHECKED_IN);
        createReservation(guest, standard, day(5), day(6), 1, "100.00", ReservationStatus.CANCELLED);

        doGet("/report/daily-movements?date=" + day(5), receptionToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpectedCheckIns").value(1))
                .andExpect(jsonPath("$.totalExpectedCheckOuts").value(1))
                .andExpect(jsonPath("$.expectedCheckIns[0].guestName").value(guest.getName()))
                .andExpect(jsonPath("$.expectedCheckIns[0].roomTypeName").value("Standard"));
    }

    // -------------------------------------------------------------- segurança

    @Test
    @DisplayName("Relatórios são exclusivos da recepção (hóspede 403, anônimo 401)")
    void reportsAreReceptionOnly() throws Exception {
        String guest = guestToken("hospede@email.com");

        for (String url : List.of(
                period("/report/occupancy", day(0), day(4)),
                period("/report/revenue", day(0), day(4)),
                "/report/daily-movements")) {
            doGet(url, guest).andExpect(status().isForbidden());
            doGet(url, null).andExpect(status().isUnauthorized());
        }
    }
}
