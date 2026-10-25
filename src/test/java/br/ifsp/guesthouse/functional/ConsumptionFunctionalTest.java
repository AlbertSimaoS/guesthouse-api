package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF10 - lançamento de consumos durante a estadia. */
@DisplayName("RF10 - Consumos")
class ConsumptionFunctionalTest extends AbstractFunctionalTest {

    private static final String BODY = "{\"type\":\"RESTAURANT\",\"description\":\"Jantar\",\"amount\":80.50}";

    private RoomType standard;
    private UserAccount guest;

    private void givenCatalog() {
        standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.OCCUPIED);
        guest = createGuest("hospede@email.com");
    }

    private Reservation reservationIn(ReservationStatus status) {
        Reservation reservation = createReservation(guest, standard, day(-1), day(1), 2, "200.00", status);
        createPayment(reservation, PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        return reservation;
    }

    private static String url(Reservation reservation) {
        return "/reservations/" + reservation.getId() + "/consumptions";
    }

    @Test
    @DisplayName("RF10: a recepção lança um consumo durante a estadia")
    void receptionRegistersConsumption() throws Exception {
        givenCatalog();
        Reservation reservation = reservationIn(ReservationStatus.CHECKED_IN);

        MvcResult result = doPost(url(reservation), receptionToken(), BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("RESTAURANT"))
                .andExpect(jsonPath("$.description").value("Jantar"))
                .andExpect(jsonPath("$.reservationId").value(reservation.getId().intValue()))
                .andReturn();

        assertThat(number(result, "$.amount")).isEqualByComparingTo("80.50");
    }

    @Test
    @DisplayName("RF10: os consumos lançados são listados com paginação")
    void consumptionsAreListed() throws Exception {
        givenCatalog();
        Reservation reservation = reservationIn(ReservationStatus.CHECKED_IN);
        String reception = receptionToken();

        for (int i = 0; i < 3; i++) {
            doPost(url(reservation), reception, BODY).andExpect(status().isCreated());
        }

        doGet(url(reservation) + "?page=0&size=2", reception)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("RF10: só a recepção lança consumos (hóspede 403, anônimo 401)")
    void onlyReceptionRegisters() throws Exception {
        givenCatalog();
        Reservation reservation = reservationIn(ReservationStatus.CHECKED_IN);

        doPost(url(reservation), login("hospede@email.com"), BODY).andExpect(status().isForbidden());
        doPost(url(reservation), null, BODY).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF10: só há consumo em estadia ativa (409 para reserva não hospedada)")
    void onlyDuringActiveStay() throws Exception {
        givenCatalog();
        Reservation confirmed = reservationIn(ReservationStatus.CONFIRMED);
        String reception = receptionToken();

        doPost(url(confirmed), reception, BODY).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RF10: depois de iniciado o check-out não é possível lançar consumos (409)")
    void notAfterCheckoutStarted() throws Exception {
        givenCatalog();
        Reservation reservation = reservationIn(ReservationStatus.CHECKED_IN);
        createPayment(reservation, PaymentType.CHECKOUT, PaymentStatus.PENDING);

        doPost(url(reservation), receptionToken(), BODY).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RF10: reserva inexistente retorna 404")
    void reservationNotFound() throws Exception {
        doPost("/reservations/9999/consumptions", receptionToken(), BODY).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF10: valor zero, negativo ou ausente e tipo ausente ou inválido retornam 400")
    void invalidPayloads() throws Exception {
        givenCatalog();
        Reservation reservation = reservationIn(ReservationStatus.CHECKED_IN);
        String reception = receptionToken();

        doPost(url(reservation), reception, "{\"type\":\"MINIBAR\",\"amount\":0}")
                .andExpect(status().isBadRequest());
        doPost(url(reservation), reception, "{\"type\":\"MINIBAR\",\"amount\":-5}")
                .andExpect(status().isBadRequest());
        doPost(url(reservation), reception, "{\"type\":\"MINIBAR\"}").andExpect(status().isBadRequest());
        doPost(url(reservation), reception, "{\"amount\":10}").andExpect(status().isBadRequest());
        doPost(url(reservation), reception, "{\"type\":\"SPA\",\"amount\":10}").andExpect(status().isBadRequest());
    }
}
