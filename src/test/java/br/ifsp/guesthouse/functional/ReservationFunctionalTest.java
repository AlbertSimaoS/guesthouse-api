package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF06 (reservar) e a parte de RF13 que existe na API (o hóspede vê as próprias reservas). */
@DisplayName("RF06/RF13 - Reservas")
class ReservationFunctionalTest extends AbstractFunctionalTest {

    private RoomType standard;

    private void givenStandardRoom(int rooms, String dailyRate) {
        standard = createRoomType("Standard", 2, dailyRate);
        for (int i = 1; i <= rooms; i++) {
            createRoom("10" + i, standard, RoomStatus.AVAILABLE);
        }
    }

    // ------------------------------------------------------------------ RF06

    @Test
    @DisplayName("RF06: hóspede reserva um tipo; nasce PENDENTE, sem quarto, com sinal de 30%")
    void guestCreatesPendingReservation() throws Exception {
        givenStandardRoom(1, "200.00");
        UserAccount guest = createGuest("hospede@email.com");
        String token = login("hospede@email.com");

        MvcResult result = doPost("/reservation", token, reservationJson(standard.getId(), day(10), day(12), 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomTypeId").value(standard.getId()))
                .andExpect(jsonPath("$.userId").value(guest.getId()))
                .andExpect(jsonPath("$.roomId").value(nullValue()))
                .andExpect(jsonPath("$.numberOfGuests").value(2))
                .andReturn();

        // 2 noites x 200,00 = 400,00; sinal = 30% = 120,00
        assertThat(number(result, "$.totalPrice")).isEqualByComparingTo("400.00");
        assertThat(number(result, "$.depositAmount")).isEqualByComparingTo("120.00");

        // cria também o pagamento do sinal, pendente
        Number reservationId = read(result, "$.id");
        Payment deposit = paymentRepository
                .findByReservationIdAndType(reservationId.longValue(), PaymentType.DEPOSIT)
                .orElseThrow();
        assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(deposit.getAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("RF06: o valor é calculado noite a noite com a tarifa da temporada de cada noite")
    void priceUsesSeasonOfEachNight() throws Exception {
        givenStandardRoom(1, "100.00");
        createSeason("Alta Temporada", day(10), day(11), "1.5");
        String token = guestToken("hospede@email.com");

        // noites: 9 (base), 10 (x1,5), 11 (x1,5) -> 100 + 150 + 150
        MvcResult result = doPost("/reservation", token, reservationJson(standard.getId(), day(9), day(12), 2))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(number(result, "$.totalPrice")).isEqualByComparingTo("400.00");
        assertThat(number(result, "$.depositAmount")).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("RF06: respeita a capacidade do tipo de quarto")
    void capacityIsEnforced() throws Exception {
        givenStandardRoom(1, "100.00");
        String token = guestToken("hospede@email.com");

        doPost("/reservation", token, reservationJson(standard.getId(), day(10), day(12), 3))
                .andExpect(status().isBadRequest());
        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    @DisplayName("RF06: não reserva quando todos os quartos do tipo já estão ocupados na noite (sem overbooking)")
    void noOverbooking() throws Exception {
        givenStandardRoom(1, "100.00");
        String first = guestToken("primeiro@email.com");
        String second = guestToken("segundo@email.com");

        doPost("/reservation", first, reservationJson(standard.getId(), day(10), day(12), 2))
                .andExpect(status().isCreated());
        doPost("/reservation", second, reservationJson(standard.getId(), day(11), day(13), 2))
                .andExpect(status().is4xxClientError());

        assertThat(reservationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF06: com 2 quartos, aceita 2 reservas na mesma data e recusa a terceira")
    void secondRoomAllowsSecondReservation() throws Exception {
        givenStandardRoom(2, "100.00");
        String a = guestToken("a@email.com");
        String b = guestToken("b@email.com");
        String c = guestToken("c@email.com");

        doPost("/reservation", a, reservationJson(standard.getId(), day(10), day(12), 2)).andExpect(status().isCreated());
        doPost("/reservation", b, reservationJson(standard.getId(), day(10), day(12), 2)).andExpect(status().isCreated());
        doPost("/reservation", c, reservationJson(standard.getId(), day(10), day(12), 2))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("RF06: tipo de quarto inexistente retorna 404")
    void unknownRoomType() throws Exception {
        String token = guestToken("hospede@email.com");

        doPost("/reservation", token, reservationJson(9999, day(10), day(12), 2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF06: dados inválidos (datas no passado, saída antes da entrada, hóspedes <= 0) retornam 400")
    void invalidReservationData() throws Exception {
        givenStandardRoom(1, "100.00");
        String token = guestToken("hospede@email.com");

        doPost("/reservation", token, reservationJson(standard.getId(), day(-3), day(2), 2))
                .andExpect(status().isBadRequest());
        doPost("/reservation", token, reservationJson(standard.getId(), day(12), day(10), 2))
                .andExpect(status().isBadRequest());
        doPost("/reservation", token, reservationJson(standard.getId(), day(10), day(10), 2))
                .andExpect(status().isBadRequest());
        doPost("/reservation", token, reservationJson(standard.getId(), day(10), day(12), 0))
                .andExpect(status().isBadRequest());
        doPost("/reservation", token, "{}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF06: em alta temporada a estadia mínima é de 2 diárias")
    void highSeasonMinimumStay() throws Exception {
        givenStandardRoom(2, "100.00");
        createSeason("Alta Temporada", day(10), day(20), "1.5");
        String token = guestToken("hospede@email.com");

        // 1 noite dentro da alta temporada: recusada
        doPost("/reservation", token, reservationJson(standard.getId(), day(12), day(13), 2))
                .andExpect(status().isBadRequest());

        // 2 noites: aceita
        doPost("/reservation", token, reservationJson(standard.getId(), day(12), day(14), 2))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RF06: fora da alta temporada vale 1 diária")
    void singleNightOutsideHighSeason() throws Exception {
        givenStandardRoom(1, "100.00");
        createSeason("Alta Temporada", day(30), day(40), "1.5");
        String token = guestToken("hospede@email.com");

        doPost("/reservation", token, reservationJson(standard.getId(), day(5), day(6), 1))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RF06: só o hóspede reserva (recepção recebe 403, anônimo 401)")
    void onlyGuestsReserve() throws Exception {
        givenStandardRoom(1, "100.00");
        String reception = receptionToken();

        doPost("/reservation", reception, reservationJson(standard.getId(), day(10), day(12), 2))
                .andExpect(status().isForbidden());
        doPost("/reservation", null, reservationJson(standard.getId(), day(10), day(12), 2))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ RF13

    @Test
    @DisplayName("RF13: o hóspede vê apenas as próprias reservas")
    void guestSeesOnlyOwnReservations() throws Exception {
        givenStandardRoom(3, "100.00");
        UserAccount maria = createGuest("maria@email.com");
        UserAccount joao = createGuest("joao@email.com");
        createReservation(maria, standard, day(10), day(12), 2, "200.00", ReservationStatus.CONFIRMED);
        createReservation(maria, standard, day(20), day(22), 2, "200.00", ReservationStatus.PENDING);
        createReservation(joao, standard, day(10), day(12), 2, "200.00", ReservationStatus.CONFIRMED);

        doGet("/reservation/my-reservations", login("maria@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(maria.getId()))
                .andExpect(jsonPath("$.content[1].userId").value(maria.getId()));

        doGet("/reservation/my-reservations", login("joao@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(joao.getId()));
    }

    @Test
    @DisplayName("RF13: hóspede sem reservas recebe lista vazia; recepção não usa essa rota (403)")
    void myReservationsEdgeCases() throws Exception {
        doGet("/reservation/my-reservations", guestToken("novo@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        doGet("/reservation/my-reservations", receptionToken()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF13: a lista de reservas é paginada")
    void myReservationsArePaged() throws Exception {
        givenStandardRoom(3, "100.00");
        UserAccount maria = createGuest("maria@email.com");
        for (int i = 0; i < 3; i++) {
            createReservation(maria, standard, day(10 + i * 5), day(12 + i * 5), 2, "200.00", ReservationStatus.CONFIRMED);
        }

        doGet("/reservation/my-reservations?page=0&size=2", login("maria@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // ----------------------------------------------------- defeitos conhecidos

    @Test
    @DisplayName("RF13: o hóspede vê o extrato da conta (diárias + consumos - sinal) durante a estadia")
    @Disabled("NÃO IMPLEMENTADO: não existe endpoint de extrato da conta (RF13) nem entidade de consumo (RF10).")
    void accountStatement() {
        // quando existir o endpoint, validar: diárias + consumos - sinal, e que o hóspede só vê o próprio extrato
    }

    @Test
    @DisplayName("RF06: estadia que cruza duas temporadas é aceita")
    @Disabled("BUG: AvailabilityService.validateHighSeasonMinimumStay usa um Optional<Season> para um intervalo; "
            + "com 2 temporadas no período o Spring Data lança IncorrectResultSizeDataAccessException (500).")
    void stayAcrossTwoSeasons() throws Exception {
        givenStandardRoom(1, "100.00");
        createSeason("Feriado", day(10), day(11), "1.3");
        createSeason("Alta Temporada", day(12), day(14), "1.5");
        String token = guestToken("hospede@email.com");

        doPost("/reservation", token, reservationJson(standard.getId(), day(10), day(14), 2))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RF06: 1 noite imediatamente antes da alta temporada não sofre a estadia mínima")
    @Disabled("BUG: validateHighSeasonMinimumStay compara season.start <= checkOut, então a noite anterior ao "
            + "início da alta temporada (checkOut = início da temporada) é tratada como alta temporada.")
    void nightBeforeHighSeasonIsNotHighSeason() throws Exception {
        givenStandardRoom(1, "100.00");
        createSeason("Alta Temporada", day(10), day(20), "1.5");
        String token = guestToken("hospede@email.com");

        doPost("/reservation", token, reservationJson(standard.getId(), day(9), day(10), 2))
                .andExpect(status().isCreated());
    }
}
