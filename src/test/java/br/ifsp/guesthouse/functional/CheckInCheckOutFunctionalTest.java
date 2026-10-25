package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Check-in (atribuição do quarto) e check-out (fechamento da conta, multa e liberação para limpeza). */
@DisplayName("Check-in e check-out")
class CheckInCheckOutFunctionalTest extends AbstractFunctionalTest {

    private RoomType standard;
    private Room room;
    private UserAccount guest;

    private void givenCatalog() {
        standard = createRoomType("Standard", 2, "100.00");
        room = createRoom("101", standard, RoomStatus.AVAILABLE);
        guest = createGuest("hospede@email.com");
    }

    /** Reserva confirmada (sinal confirmado) que começa em {@code startInDays}. */
    private Reservation confirmed(int startInDays, int endInDays, String total) {
        Reservation reservation = createReservation(
                guest, standard, day(startInDays), day(endInDays), 2, total, ReservationStatus.CONFIRMED);
        createPayment(reservation, PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        return reservation;
    }

    /** Hóspede já hospedado no quarto {@link #room}, com sinal confirmado. */
    private Reservation checkedIn(int startInDays, int endInDays, String total) {
        Reservation reservation = createReservation(
                guest, standard, day(startInDays), day(endInDays), 2, total, ReservationStatus.CHECKED_IN);
        createPayment(reservation, PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        reservation.setRoom(room);
        return reservationRepository.save(reservation);
    }

    private static String checkInBody(Room room) {
        return "{\"roomId\":%d}".formatted(room.getId());
    }

    private RoomStatus statusOf(Room target) {
        return roomRepository.findById(target.getId()).orElseThrow().getStatus();
    }

    private ReservationStatus statusOf(Reservation target) {
        return reservationRepository.findById(target.getId()).orElseThrow().getStatus();
    }

    // --------------------------------------------------------------- check-in

    @Test
    @DisplayName("check-in: a recepção atribui um quarto livre do tipo reservado na data de entrada")
    void receptionChecksIn() throws Exception {
        givenCatalog();
        Reservation reservation = confirmed(0, 2, "200.00");

        doPatch("/reservation/" + reservation.getId() + "/check-in", receptionToken(), checkInBody(room))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.roomId").value(room.getId()));

        assertThat(statusOf(reservation)).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(statusOf(room)).isEqualTo(RoomStatus.OCCUPIED);
    }

    @Test
    @DisplayName("check-in: só a recepção faz (hóspede 403, anônimo 401)")
    void onlyReceptionChecksIn() throws Exception {
        givenCatalog();
        Reservation reservation = confirmed(0, 2, "200.00");
        String url = "/reservation/" + reservation.getId() + "/check-in";

        doPatch(url, login("hospede@email.com"), checkInBody(room)).andExpect(status().isForbidden());
        doPatch(url, null, checkInBody(room)).andExpect(status().isUnauthorized());

        assertThat(statusOf(reservation)).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("check-in: só é permitido na data de entrada")
    void checkInOnlyOnStartDate() throws Exception {
        givenCatalog();
        Reservation tomorrow = confirmed(1, 3, "200.00");

        doPatch("/reservation/" + tomorrow.getId() + "/check-in", receptionToken(), checkInBody(room))
                .andExpect(status().isConflict());

        assertThat(statusOf(room)).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("check-in: reserva pendente (sinal não pago) não faz check-in")
    void pendingReservationCannotCheckIn() throws Exception {
        givenCatalog();
        Reservation pending = createReservation(
                guest, standard, day(0), day(2), 2, "200.00", ReservationStatus.PENDING);
        createPayment(pending, PaymentType.DEPOSIT, PaymentStatus.PENDING);

        doPatch("/reservation/" + pending.getId() + "/check-in", receptionToken(), checkInBody(room))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("check-in: quarto de outro tipo é recusado (400)")
    void roomOfAnotherTypeIsRejected() throws Exception {
        givenCatalog();
        RoomType suite = createRoomType("Suite", 3, "300.00");
        Room suiteRoom = createRoom("201", suite, RoomStatus.AVAILABLE);
        Reservation reservation = confirmed(0, 2, "200.00");

        doPatch("/reservation/" + reservation.getId() + "/check-in", receptionToken(), checkInBody(suiteRoom))
                .andExpect(status().isBadRequest());

        assertThat(statusOf(suiteRoom)).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("check-in: quarto que não está livre é recusado (409)")
    void unavailableRoomIsRejected() throws Exception {
        givenCatalog();
        Room cleaning = createRoom("102", standard, RoomStatus.CLEANING);
        Reservation reservation = confirmed(0, 2, "200.00");

        doPatch("/reservation/" + reservation.getId() + "/check-in", receptionToken(), checkInBody(cleaning))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("check-in: reserva ou quarto inexistente retorna 404; corpo sem roomId retorna 400")
    void checkInNotFoundAndInvalid() throws Exception {
        givenCatalog();
        Reservation reservation = confirmed(0, 2, "200.00");
        String reception = receptionToken();

        doPatch("/reservation/9999/check-in", reception, checkInBody(room)).andExpect(status().isNotFound());
        doPatch("/reservation/" + reservation.getId() + "/check-in", reception, "{\"roomId\":9999}")
                .andExpect(status().isNotFound());
        doPatch("/reservation/" + reservation.getId() + "/check-in", reception, "{}")
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- check-out

    @Test
    @DisplayName("check-out na data prevista, sem consumos: diárias - sinal, sem multa")
    void regularCheckoutWithoutConsumptions() throws Exception {
        givenCatalog();
        Reservation reservation = checkedIn(-2, 0, "200.00"); // sinal de 60,00

        MvcResult result = doPatch("/reservation/" + reservation.getId() + "/check-out", receptionToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earlyCheckout").value(false))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andReturn();

        assertThat(number(result, "$.stayPrice")).isEqualByComparingTo("200.00");
        assertThat(number(result, "$.consumptionTotal")).isEqualByComparingTo("0.00");
        assertThat(number(result, "$.penalty")).isEqualByComparingTo("0.00");
        assertThat(number(result, "$.depositAmount")).isEqualByComparingTo("60.00");
        assertThat(number(result, "$.totalDue")).isEqualByComparingTo("200.00");
        assertThat(number(result, "$.checkoutAmount")).isEqualByComparingTo("140.00");

        Payment checkout = paymentRepository
                .findByReservationIdAndType(reservation.getId(), PaymentType.CHECKOUT)
                .orElseThrow();
        assertThat(checkout.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(checkout.getAmount()).isEqualByComparingTo("140.00");
    }

    @Test
    @DisplayName("check-out: os consumos lançados entram na conta")
    void checkoutIncludesConsumptions() throws Exception {
        givenCatalog();
        Reservation reservation = checkedIn(-2, 0, "200.00");
        String reception = receptionToken();

        doPost("/reservations/" + reservation.getId() + "/consumptions", reception,
                "{\"type\":\"MINIBAR\",\"description\":\"Agua\",\"amount\":50.00}")
                .andExpect(status().isCreated());

        MvcResult result = doPatch("/reservation/" + reservation.getId() + "/check-out", reception, null)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(number(result, "$.consumptionTotal")).isEqualByComparingTo("50.00");
        assertThat(number(result, "$.totalDue")).isEqualByComparingTo("250.00");
        assertThat(number(result, "$.checkoutAmount")).isEqualByComparingTo("190.00");
    }

    @Test
    @DisplayName("check-out antecipado: cobra só as noites usadas mais uma noite de multa")
    void earlyCheckoutChargesUsedNightsPlusPenalty() throws Exception {
        givenCatalog();
        // 5 noites previstas (500,00, sinal 150,00); hóspede entrou há 2 dias e sai hoje
        Reservation reservation = checkedIn(-2, 3, "500.00");

        MvcResult result = doPatch("/reservation/" + reservation.getId() + "/check-out", receptionToken(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earlyCheckout").value(true))
                .andReturn();

        assertThat(number(result, "$.stayPrice")).isEqualByComparingTo("200.00");   // 2 noites usadas
        assertThat(number(result, "$.penalty")).isEqualByComparingTo("100.00");     // 1 noite de multa
        assertThat(number(result, "$.totalDue")).isEqualByComparingTo("300.00");
        assertThat(number(result, "$.checkoutAmount")).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("check-out: confirmar o pagamento finaliza a reserva e envia o quarto para limpeza")
    void confirmingCheckoutPaymentFinishesStay() throws Exception {
        givenCatalog();
        Reservation reservation = checkedIn(-2, 0, "200.00");
        String reception = receptionToken();

        MvcResult checkout = doPatch("/reservation/" + reservation.getId() + "/check-out", reception, null)
                .andExpect(status().isOk())
                .andReturn();
        Number paymentId = read(checkout, "$.paymentId");

        doPatch("/payments/" + paymentId.longValue(), reception, "{\"status\":\"CONFIRMED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CHECKOUT"));

        assertThat(statusOf(reservation)).isEqualTo(ReservationStatus.FINISHED);
        assertThat(statusOf(room)).isEqualTo(RoomStatus.CLEANING);
    }

    @Test
    @DisplayName("check-out: só a recepção faz (hóspede 403, anônimo 401)")
    void onlyReceptionChecksOut() throws Exception {
        givenCatalog();
        Reservation reservation = checkedIn(-2, 0, "200.00");
        String url = "/reservation/" + reservation.getId() + "/check-out";

        doPatch(url, login("hospede@email.com"), null).andExpect(status().isForbidden());
        doPatch(url, null, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("check-out: reserva que não está hospedada (409) ou inexistente (404)")
    void checkoutRequiresCheckedInReservation() throws Exception {
        givenCatalog();
        Reservation reservation = confirmed(0, 2, "200.00");
        String reception = receptionToken();

        doPatch("/reservation/" + reservation.getId() + "/check-out", reception, null)
                .andExpect(status().isConflict());
        doPatch("/reservation/9999/check-out", reception, null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("check-out: exige o sinal confirmado (409)")
    void checkoutRequiresConfirmedDeposit() throws Exception {
        givenCatalog();
        Reservation reservation = createReservation(
                guest, standard, day(-2), day(0), 2, "200.00", ReservationStatus.CHECKED_IN);
        createPayment(reservation, PaymentType.DEPOSIT, PaymentStatus.PENDING);
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        reservation.setRoom(room);
        reservationRepository.save(reservation);

        doPatch("/reservation/" + reservation.getId() + "/check-out", receptionToken(), null)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("check-out: saldo zero ou negativo (sinal cobre tudo) é recusado com 409")
    void checkoutWithNonPositiveBalanceIsRejected() throws Exception {
        givenCatalog();
        Reservation reservation = checkedIn(-2, 0, "200.00");
        Payment deposit = paymentRepository
                .findByReservationIdAndType(reservation.getId(), PaymentType.DEPOSIT)
                .orElseThrow();
        deposit.setAmount(new BigDecimal("300.00"));
        paymentRepository.save(deposit);

        doPatch("/reservation/" + reservation.getId() + "/check-out", receptionToken(), null)
                .andExpect(status().isConflict());

        assertThat(paymentRepository.existsByReservationIdAndType(reservation.getId(), PaymentType.CHECKOUT))
                .isFalse();
    }
}
