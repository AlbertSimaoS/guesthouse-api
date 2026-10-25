package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF05 - consulta de disponibilidade e valor total da estadia. */
@DisplayName("RF05 - Disponibilidade")
class AvailabilityFunctionalTest extends AbstractFunctionalTest {

    @Test
    @DisplayName("RF05: qualquer usuário (inclusive anônimo) consulta disponibilidade e o valor da estadia")
    void anonymousCanQueryAvailability() throws Exception {
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createRoom("102", standard, RoomStatus.AVAILABLE);

        MvcResult result = doGet(availabilityUrl(day(10), day(13), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].roomTypeId").value(standard.getId()))
                .andExpect(jsonPath("$.content[0].roomTypeName").value("Standard"))
                .andExpect(jsonPath("$.content[0].availableRooms").value(2))
                .andReturn();

        // 3 noites x 100,00, sem temporada = diária base
        assertThat(number(result, "$.content[0].totalStayPrice")).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("RF05: hóspede e recepção também consultam")
    void authenticatedUsersCanQueryAvailability() throws Exception {
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);

        doGet(availabilityUrl(day(10), day(12), 2), guestToken("hospede@email.com")).andExpect(status().isOk());
        doGet(availabilityUrl(day(10), day(12), 2), receptionToken()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF05: valor calculado noite a noite com o multiplicador da temporada de cada noite")
    void priceIsComputedNightByNight() throws Exception {
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        // noites 10 e 11 em temporada (x1,5); noite 9 fora (diária base)
        createSeason("Alta Temporada", day(10), day(11), "1.5");

        MvcResult result = doGet(availabilityUrl(day(9), day(12), 2), null)
                .andExpect(status().isOk())
                .andReturn();

        // 100 + 150 + 150
        assertThat(number(result, "$.content[0].totalStayPrice")).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("RF05: estadia fora de qualquer temporada usa só a diária base")
    void priceOutsideSeasonUsesBaseRate() throws Exception {
        RoomType standard = createRoomType("Standard", 2, "120.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createSeason("Alta Temporada", day(50), day(60), "2.0");

        MvcResult result = doGet(availabilityUrl(day(10), day(12), 1), null).andReturn();

        assertThat(number(result, "$.content[0].totalStayPrice")).isEqualByComparingTo("240.00");
    }

    @Test
    @DisplayName("RF05: tipos que não comportam o número de hóspedes não aparecem")
    void capacityFiltersRoomTypes() throws Exception {
        RoomType small = createRoomType("Solteiro", 1, "80.00");
        RoomType big = createRoomType("Familia", 4, "300.00");
        createRoom("101", small, RoomStatus.AVAILABLE);
        createRoom("201", big, RoomStatus.AVAILABLE);

        doGet(availabilityUrl(day(10), day(12), 3), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].roomTypeName").value("Familia"));
    }

    @Test
    @DisplayName("RF05: tipo com todos os quartos reservados na noite não aparece; outros tipos sim")
    void fullyBookedTypeIsExcluded() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        RoomType suite = createRoomType("Suite", 2, "300.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createRoom("201", suite, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(10), day(13), 2, "300.00", ReservationStatus.CONFIRMED);

        doGet(availabilityUrl(day(11), day(12), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].roomTypeName").value("Suite"));
    }

    @Test
    @DisplayName("RF05: reserva cancelada ou expirada não ocupa a noite")
    void cancelledAndExpiredReservationsDoNotBlock() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(10), day(13), 2, "300.00", ReservationStatus.CANCELLED);
        createReservation(guest, standard, day(10), day(13), 2, "300.00", ReservationStatus.EXPIRED);

        doGet(availabilityUrl(day(10), day(13), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].availableRooms").value(1));
    }

    @Test
    @DisplayName("RF05: reserva pendente também segura o quarto")
    void pendingReservationBlocks() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(10), day(13), 2, "300.00", ReservationStatus.PENDING);

        doGet(availabilityUrl(day(10), day(13), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("RF05: tipo sem nenhum quarto cadastrado não aparece")
    void typeWithoutRoomsIsExcluded() throws Exception {
        createRoomType("Standard", 2, "100.00");

        doGet(availabilityUrl(day(10), day(13), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("RF05: o resultado é paginado")
    void resultIsPaged() throws Exception {
        for (int i = 1; i <= 3; i++) {
            RoomType type = createRoomType("Tipo " + i, 2, "100.00");
            createRoom("10" + i, type, RoomStatus.AVAILABLE);
        }

        doGet(availabilityUrl(day(10), day(12), 2) + "&page=0&size=2", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("RF05: parâmetros inválidos retornam 400")
    void invalidParameters() throws Exception {
        // saída antes/igual à entrada
        doGet(availabilityUrl(day(12), day(10), 2), null).andExpect(status().isBadRequest());
        doGet(availabilityUrl(day(10), day(10), 2), null).andExpect(status().isBadRequest());
        // nº de hóspedes inválido
        doGet(availabilityUrl(day(10), day(12), 0), null).andExpect(status().isBadRequest());
        // datas no passado
        doGet(availabilityUrl(day(-5), day(-3), 2), null).andExpect(status().isBadRequest());
        // parâmetros ausentes
        doGet("/room-types/availability", null).andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------- defeitos conhecidos

    @Test
    @DisplayName("RF05: quarto em manutenção não conta na disponibilidade")
    void maintenanceRoomsAreNotCounted() throws Exception {
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.MAINTENANCE);
        createRoom("102", standard, RoomStatus.AVAILABLE);
        UserAccount guest = createGuest("hospede@email.com");
        createReservation(guest, standard, day(10), day(13), 2, "300.00", ReservationStatus.CONFIRMED);

        // 2 quartos, 1 em manutenção, 1 reservado -> nenhum disponível
        doGet(availabilityUrl(day(10), day(13), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("RF05: estadia que começa no dia em que outra termina (check-out = check-in) está disponível")
    void backToBackStaysAreAvailable() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(10), day(12), 2, "200.00", ReservationStatus.CONFIRMED);

        doGet(availabilityUrl(day(12), day(14), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("RF05: a disponibilidade é por noite (duas reservas em sequência não esgotam 2 quartos)")
    void availabilityIsEvaluatedPerNight() throws Exception {
        UserAccount guest = createGuest("hospede@email.com");
        RoomType standard = createRoomType("Standard", 2, "100.00");
        createRoom("101", standard, RoomStatus.AVAILABLE);
        createRoom("102", standard, RoomStatus.AVAILABLE);
        createReservation(guest, standard, day(10), day(12), 2, "200.00", ReservationStatus.CONFIRMED);
        createReservation(guest, standard, day(12), day(14), 2, "200.00", ReservationStatus.CONFIRMED);

        // em toda noite de 10 a 14 há no máximo 1 quarto ocupado -> ainda há 1 livre
        doGet(availabilityUrl(day(10), day(14), 2), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].availableRooms").value(1));
    }
}
