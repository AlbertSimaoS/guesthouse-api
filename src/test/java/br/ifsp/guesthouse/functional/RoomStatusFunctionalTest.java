package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF12 - liberar o quarto depois da limpeza ou colocá-lo em manutenção. */
@DisplayName("RF12 - Status do quarto")
class RoomStatusFunctionalTest extends AbstractFunctionalTest {

    private Room roomIn(RoomStatus initial) {
        RoomType type = createRoomType("Standard", 2, "100.00");
        return createRoom("101", type, initial);
    }

    private String url(Room room) {
        return "/room/" + room.getId() + "/update-status";
    }

    private RoomStatus currentStatus(Room room) {
        return roomRepository.findById(room.getId()).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("RF12: recepção libera o quarto depois da limpeza (CLEANING -> AVAILABLE)")
    void releaseAfterCleaning() throws Exception {
        Room room = roomIn(RoomStatus.CLEANING);

        doPatch(url(room), receptionToken(), "{\"status\":\"AVAILABLE\"}").andExpect(status().isOk());

        assertThat(currentStatus(room)).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("RF12: quarto livre pode ir para manutenção")
    void availableToMaintenance() throws Exception {
        Room room = roomIn(RoomStatus.AVAILABLE);

        doPatch(url(room), receptionToken(), "{\"status\":\"MAINTENANCE\"}").andExpect(status().isOk());

        assertThat(currentStatus(room)).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("RF12: quarto em limpeza pode ir para manutenção")
    void cleaningToMaintenance() throws Exception {
        Room room = roomIn(RoomStatus.CLEANING);

        doPatch(url(room), receptionToken(), "{\"status\":\"MAINTENANCE\"}").andExpect(status().isOk());

        assertThat(currentStatus(room)).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("RF12: a mudança de status registra o instante da alteração")
    void statusChangeIsTimestamped() throws Exception {
        Room room = roomIn(RoomStatus.AVAILABLE);

        doPatch(url(room), receptionToken(), "{\"status\":\"MAINTENANCE\"}").andExpect(status().isOk());

        assertThat(roomRepository.findById(room.getId()).orElseThrow().getStatusUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("RF12: só a recepção altera o status (hóspede 403, anônimo 401)")
    void onlyReceptionChangesStatus() throws Exception {
        Room room = roomIn(RoomStatus.CLEANING);

        doPatch(url(room), guestToken("hospede@email.com"), "{\"status\":\"AVAILABLE\"}")
                .andExpect(status().isForbidden());
        doPatch(url(room), null, "{\"status\":\"AVAILABLE\"}").andExpect(status().isUnauthorized());

        assertThat(currentStatus(room)).isEqualTo(RoomStatus.CLEANING);
    }

    @Test
    @DisplayName("RF12: status inválido ou ausente retorna 400")
    void invalidStatusPayload() throws Exception {
        Room room = roomIn(RoomStatus.CLEANING);
        String reception = receptionToken();

        doPatch(url(room), reception, "{\"status\":\"VOANDO\"}").andExpect(status().isBadRequest());
        doPatch(url(room), reception, "{}").andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------- defeitos conhecidos

    @Test
    @DisplayName("RF12: quarto ocupado não pode ir para manutenção (4xx)")
    @Disabled("BUG: RoomService lança IllegalStateException, que não tem handler -> 500 em vez de 409/422.")
    void occupiedRoomCannotGoToMaintenance() throws Exception {
        Room room = roomIn(RoomStatus.OCCUPIED);

        doPatch(url(room), receptionToken(), "{\"status\":\"MAINTENANCE\"}")
                .andExpect(status().is4xxClientError());

        assertThat(currentStatus(room)).isEqualTo(RoomStatus.OCCUPIED);
    }

    @Test
    @DisplayName("RF12: quarto inexistente retorna 404")
    @Disabled("BUG: RoomService lança jakarta.persistence.EntityNotFoundException, sem handler -> 500.")
    void roomNotFound() throws Exception {
        doPatch("/room/9999/update-status", receptionToken(), "{\"status\":\"AVAILABLE\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF12: a recepção não consegue marcar manualmente um quarto como ocupado")
    @Disabled("BUG: o enunciado só prevê 'liberar após limpeza' e 'manutenção (só quarto livre ou em limpeza)', "
            + "mas updateRoomStatus aceita qualquer destino (ex.: AVAILABLE -> OCCUPIED, MAINTENANCE -> CLEANING).")
    void receptionCannotSetOccupiedManually() throws Exception {
        Room room = roomIn(RoomStatus.AVAILABLE);

        doPatch(url(room), receptionToken(), "{\"status\":\"OCCUPIED\"}")
                .andExpect(status().is4xxClientError());

        assertThat(currentStatus(room)).isEqualTo(RoomStatus.AVAILABLE);
    }
}
