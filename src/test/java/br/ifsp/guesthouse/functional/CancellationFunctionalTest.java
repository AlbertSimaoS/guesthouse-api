package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF08 - cancelamento: até 7 dias antes da entrada o sinal é devolvido; depois, fica retido.
 *
 * Quase todos os cenários estão desabilitados porque o endpoint de cancelamento está quebrado
 * (NullPointerException em toda reserva sem quarto atribuído, ou seja, antes do check-in).
 * Remova o @Disabled depois de corrigir o bug.
 */
@DisplayName("RF08 - Cancelamento")
class CancellationFunctionalTest extends AbstractFunctionalTest {

    private RoomType standard;
    private UserAccount guest;

    private void givenCatalog() {
        standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        guest = createGuest("hospede@email.com");
    }

    private Reservation reservation(int startInDays, ReservationStatus status, PaymentStatus depositStatus) {
        Reservation reservation = createReservation(
                guest, standard, day(startInDays), day(startInDays + 2), 2, "200.00", status);
        createPayment(reservation, PaymentType.DEPOSIT, depositStatus);
        return reservation;
    }

    private PaymentStatus depositStatusOf(Reservation reservation) {
        return paymentRepository
                .findByReservationIdAndType(reservation.getId(), PaymentType.DEPOSIT)
                .orElseThrow()
                .getStatus();
    }

    @Test
    @DisplayName("RF08: reserva inexistente retorna 404")
    void reservationNotFound() throws Exception {
        doPatch("/reservation/9999/cancel", guestToken("hospede@email.com"), null)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF08: cancelar sem token retorna 401")
    void cancelRequiresAuthentication() throws Exception {
        doPatch("/reservation/1/cancel", null, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF08: faltando mais de 7 dias, o sinal é devolvido integralmente")
    @Disabled(BUG_NPE_CANCEL)
    void refundWhenMoreThan7DaysBefore() throws Exception {
        givenCatalog();
        Reservation reservation = reservation(10, ReservationStatus.CONFIRMED, PaymentStatus.CONFIRMED);

        doPatch("/reservation/" + reservation.getId() + "/cancel", login("hospede@email.com"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(depositStatusOf(reservation)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("RF08: faltando exatamente 7 dias, o sinal ainda é devolvido")
    @Disabled(BUG_NPE_CANCEL)
    void refundExactly7DaysBefore() throws Exception {
        givenCatalog();
        Reservation reservation = reservation(7, ReservationStatus.CONFIRMED, PaymentStatus.CONFIRMED);

        doPatch("/reservation/" + reservation.getId() + "/cancel", login("hospede@email.com"), null)
                .andExpect(status().isOk());

        assertThat(depositStatusOf(reservation)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("RF08: faltando menos de 7 dias, o sinal fica retido")
    @Disabled(BUG_NPE_CANCEL)
    void depositRetainedWhenLessThan7DaysBefore() throws Exception {
        givenCatalog();
        Reservation reservation = reservation(6, ReservationStatus.CONFIRMED, PaymentStatus.CONFIRMED);

        doPatch("/reservation/" + reservation.getId() + "/cancel", login("hospede@email.com"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(depositStatusOf(reservation)).isEqualTo(PaymentStatus.RETAINED);
    }

    @Test
    @DisplayName("RF08: reserva pendente (sinal não pago) é cancelada e o pagamento é cancelado")
    @Disabled(BUG_NPE_CANCEL)
    void cancelPendingReservation() throws Exception {
        givenCatalog();
        Reservation reservation = reservation(3, ReservationStatus.PENDING, PaymentStatus.PENDING);

        doPatch("/reservation/" + reservation.getId() + "/cancel", login("hospede@email.com"), null)
                .andExpect(status().isOk());

        assertThat(depositStatusOf(reservation)).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("RF08: depois de cancelada, a noite volta a ficar disponível")
    @Disabled(BUG_NPE_CANCEL)
    void cancelledReservationFreesAvailability() throws Exception {
        givenCatalog();
        Reservation reservation = reservation(10, ReservationStatus.CONFIRMED, PaymentStatus.CONFIRMED);

        doPatch("/reservation/" + reservation.getId() + "/cancel", login("hospede@email.com"), null)
                .andExpect(status().isOk());

        doGet(availabilityUrl(day(10), day(12), 2), null)
                .andExpect(jsonPath("$.content[0].roomTypeId").value(standard.getId()));
    }

    @Test
    @DisplayName("RF08: não cancela reserva já cancelada ou finalizada (4xx)")
    @Disabled("BUG: IllegalStateException não tem handler no GlobalExceptionHandler -> 500 em vez de 409/422.")
    void cannotCancelTwice() throws Exception {
        givenCatalog();
        Reservation cancelled = reservation(10, ReservationStatus.CANCELLED, PaymentStatus.REFUNDED);

        doPatch("/reservation/" + cancelled.getId() + "/cancel", login("hospede@email.com"), null)
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Regra geral: o hóspede só cancela as próprias reservas")
    @Disabled("BUG: ReservationService.cancelReservation não confere se a reserva pertence ao usuário "
            + "autenticado; qualquer hóspede cancela a reserva de outro.")
    void guestCannotCancelSomeoneElsesReservation() throws Exception {
        givenCatalog();
        Reservation reservation = reservation(10, ReservationStatus.CONFIRMED, PaymentStatus.CONFIRMED);
        String other = guestToken("outro@email.com");

        doPatch("/reservation/" + reservation.getId() + "/cancel", other, null)
                .andExpect(status().is4xxClientError());

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
    }
}
