package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Payment;
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

/** RF07 - sinal de 30%, confirmação da reserva e expiração das pendentes. */
@DisplayName("RF07 - Sinal e expiração")
class DepositAndExpirationFunctionalTest extends AbstractFunctionalTest {

    private RoomType standard;
    private UserAccount guest;

    private void givenCatalog() {
        standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        guest = createGuest("hospede@email.com");
    }

    private Reservation pendingReservationWithDeposit() {
        Reservation reservation = createReservation(
                guest, standard, day(10), day(12), 2, "200.00", ReservationStatus.PENDING);
        createPayment(reservation, PaymentType.DEPOSIT, PaymentStatus.PENDING);
        return reservation;
    }

    private Long depositId(Reservation reservation) {
        return paymentRepository
                .findByReservationIdAndType(reservation.getId(), PaymentType.DEPOSIT)
                .orElseThrow()
                .getId();
    }

    // --------------------------------------------------- registrar o sinal

    @Test
    @DisplayName("RF07: a recepção registra o sinal e a reserva passa de PENDENTE para CONFIRMADA")
    void receptionRegistersDeposit() throws Exception {
        givenCatalog();
        Reservation reservation = pendingReservationWithDeposit();
        String reception = receptionToken();

        doPatch("/payments/" + depositId(reservation), reception, "{\"status\":\"CONFIRMED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.reservationId").value(reservation.getId()));

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(paymentRepository.findById(depositId(reservation)).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("RF07: fluxo completo - hóspede reserva pela API e a recepção confirma o sinal")
    void endToEndReserveThenPayDeposit() throws Exception {
        givenCatalog();
        String guestToken = login("hospede@email.com");
        String reception = receptionToken();

        MvcResult created = doPost("/reservation", guestToken, reservationJson(standard.getId(), day(10), day(12), 2))
                .andExpect(status().isCreated())
                .andReturn();
        Number reservationId = read(created, "$.id");

        Long paymentId = paymentRepository
                .findByReservationIdAndType(reservationId.longValue(), PaymentType.DEPOSIT)
                .orElseThrow()
                .getId();
        doPatch("/payments/" + paymentId, reception, "{\"status\":\"CONFIRMED\"}").andExpect(status().isOk());

        doGet("/reservation/my-reservations", guestToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("RF07: só aceita o status CONFIRMED; valores inválidos retornam 400")
    void onlyConfirmedIsAccepted() throws Exception {
        givenCatalog();
        Reservation reservation = pendingReservationWithDeposit();
        String reception = receptionToken();
        String url = "/payments/" + depositId(reservation);

        doPatch(url, reception, "{\"status\":\"REFUNDED\"}").andExpect(status().isBadRequest());
        doPatch(url, reception, "{\"status\":\"BANANA\"}").andExpect(status().isBadRequest());
        doPatch(url, reception, "{}").andExpect(status().isBadRequest());

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("RF07: pagamento inexistente retorna 404")
    void paymentNotFound() throws Exception {
        doPatch("/payments/9999", receptionToken(), "{\"status\":\"CONFIRMED\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF07: sem token a rota retorna 401")
    void paymentRequiresAuthentication() throws Exception {
        doPatch("/payments/1", null, "{\"status\":\"CONFIRMED\"}").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF07: o hóspede NÃO pode registrar o próprio sinal (403)")
    void guestCannotRegisterDeposit() throws Exception {
        givenCatalog();
        Reservation reservation = pendingReservationWithDeposit();

        doPatch("/payments/" + depositId(reservation), login("hospede@email.com"), "{\"status\":\"CONFIRMED\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF07: registrar o sinal duas vezes é rejeitado com 4xx")
    void depositCannotBeRegisteredTwice() throws Exception {
        givenCatalog();
        Reservation reservation = pendingReservationWithDeposit();
        String reception = receptionToken();
        String url = "/payments/" + depositId(reservation);

        doPatch(url, reception, "{\"status\":\"CONFIRMED\"}").andExpect(status().isOk());
        doPatch(url, reception, "{\"status\":\"CONFIRMED\"}").andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------ expiração

    @Test
    @DisplayName("RF07: reserva pendente há mais de 24h expira quando a recepção processa as expiradas")
    void expiresPendingReservationsOlderThan24h() throws Exception {
        givenCatalog();
        Reservation old = pendingReservationWithDeposit();
        backdateReservation(old.getId(), 25);

        doPatch("/reservation/expired", receptionToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredReservations").value(1));

        assertThat(reservationRepository.findById(old.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);
        Payment deposit = paymentRepository.findById(depositId(old)).orElseThrow();
        assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    @DisplayName("RF07: reserva já confirmada não expira, mesmo antiga")
    void confirmedReservationsDoNotExpire() throws Exception {
        givenCatalog();
        Reservation confirmed = createReservation(
                guest, standard, day(10), day(12), 2, "200.00", ReservationStatus.CONFIRMED);
        createPayment(confirmed, PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        backdateReservation(confirmed.getId(), 72);

        doPatch("/reservation/expired", receptionToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredReservations").value(0));

        assertThat(reservationRepository.findById(confirmed.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("RF07: depois de expirar, o quarto volta a ficar disponível")
    void expiredReservationFreesAvailability() throws Exception {
        givenCatalog();
        Reservation old = pendingReservationWithDeposit();
        backdateReservation(old.getId(), 30);

        doGet(availabilityUrl(day(10), day(12), 2), null)
                .andExpect(jsonPath("$.content", hasSize(0)));

        doPatch("/reservation/expired", receptionToken(), null).andExpect(status().isOk());

        doGet(availabilityUrl(day(10), day(12), 2), null)
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("RF07: sem nada para expirar o contador é zero")
    void nothingToExpire() throws Exception {
        doPatch("/reservation/expired", receptionToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredReservations").value(0));
    }

    @Test
    @DisplayName("RF07: reserva pendente há menos de 24h NÃO expira")
    void recentPendingReservationsDoNotExpire() throws Exception {
        givenCatalog();
        Reservation recent = pendingReservationWithDeposit();
        backdateReservation(recent.getId(), 23);

        doPatch("/reservation/expired", receptionToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredReservations").value(0));

        assertThat(reservationRepository.findById(recent.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("RF07: só a recepção processa as expiradas (hóspede recebe 403)")
    void onlyReceptionProcessesExpired() throws Exception {
        doPatch("/reservation/expired", guestToken("hospede@email.com"), null)
                .andExpect(status().isForbidden());
    }
}
