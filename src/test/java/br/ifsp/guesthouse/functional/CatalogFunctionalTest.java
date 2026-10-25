package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF03 (tipos de quarto e quartos) e RF04 (temporadas). */
@DisplayName("RF03/RF04 - Cadastros da recepção")
class CatalogFunctionalTest extends AbstractFunctionalTest {

    private static final String ROOM_TYPE_JSON =
            "{\"name\":\"Duplo vista mar\",\"capacity\":2,\"dailyRate\":350.00}";

    // ------------------------------------------------------------------ RF03

    @Test
    @DisplayName("RF03: recepção cadastra tipo de quarto (nome, capacidade, diária base)")
    void receptionCreatesRoomType() throws Exception {
        String token = receptionToken();

        doPost("/room-types/register", token, ROOM_TYPE_JSON)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Duplo vista mar"))
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.dailyRate").value(350.00));

        assertThat(roomTypeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF03: hóspede não cadastra tipo de quarto (403) e anônimo recebe 401")
    void onlyReceptionCreatesRoomType() throws Exception {
        String guest = guestToken("hospede@email.com");

        doPost("/room-types/register", guest, ROOM_TYPE_JSON).andExpect(status().isForbidden());
        doPost("/room-types/register", null, ROOM_TYPE_JSON).andExpect(status().isUnauthorized());
        assertThat(roomTypeRepository.count()).isZero();
    }

    @Test
    @DisplayName("RF03: tipo de quarto com dados inválidos retorna 400")
    void roomTypeValidation() throws Exception {
        String token = receptionToken();

        doPost("/room-types/register", token, "{\"name\":\"\",\"capacity\":2,\"dailyRate\":100}")
                .andExpect(status().isBadRequest());
        doPost("/room-types/register", token, "{\"name\":\"Simples\",\"capacity\":0,\"dailyRate\":100}")
                .andExpect(status().isBadRequest());
        doPost("/room-types/register", token, "{\"name\":\"Simples\",\"capacity\":1,\"dailyRate\":0}")
                .andExpect(status().isBadRequest());
        doPost("/room-types/register", token, "{\"name\":\"Simples\",\"capacity\":1}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF03: não permite dois tipos com o mesmo nome")
    void roomTypeDuplicateName() throws Exception {
        String token = receptionToken();

        doPost("/room-types/register", token, ROOM_TYPE_JSON).andExpect(status().isCreated());
        doPost("/room-types/register", token, ROOM_TYPE_JSON.replace("Duplo vista mar", "DUPLO VISTA MAR"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("RF03: recepção cadastra quarto (número único + tipo); nasce livre")
    void receptionCreatesRoom() throws Exception {
        String token = receptionToken();
        RoomType type = createRoomType("Standard", 2, "200.00");

        doPost("/room/register", token, "{\"number\":\"101\",\"typeId\":%d}".formatted(type.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("101"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.typeId").value(type.getId()))
                .andExpect(jsonPath("$.typeName").value("Standard"));
    }

    @Test
    @DisplayName("RF03: número de quarto duplicado é rejeitado")
    void roomDuplicateNumber() throws Exception {
        String token = receptionToken();
        RoomType type = createRoomType("Standard", 2, "200.00");
        String body = "{\"number\":\"101\",\"typeId\":%d}".formatted(type.getId());

        doPost("/room/register", token, body).andExpect(status().isCreated());
        doPost("/room/register", token, body).andExpect(status().is4xxClientError());
        assertThat(roomRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF03: quarto com tipo inexistente ou número inválido é rejeitado")
    void roomValidation() throws Exception {
        String token = receptionToken();

        doPost("/room/register", token, "{\"number\":\"101\",\"typeId\":9999}")
                .andExpect(status().is4xxClientError());
        doPost("/room/register", token, "{\"number\":\"\",\"typeId\":1}")
                .andExpect(status().isBadRequest());
        doPost("/room/register", token, "{\"number\":\"1234\",\"typeId\":1}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF03: hóspede não cadastra quarto")
    void guestCannotCreateRoom() throws Exception {
        String guest = guestToken("hospede@email.com");
        RoomType type = createRoomType("Standard", 2, "200.00");

        doPost("/room/register", guest, "{\"number\":\"101\",\"typeId\":%d}".formatted(type.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF03: um quarto pode estar livre, ocupado, em limpeza ou em manutenção (e persiste o estado)")
    void roomStatusesArePersisted() {
        RoomType type = createRoomType("Standard", 2, "200.00");

        for (RoomStatus status : RoomStatus.values()) {
            var room = createRoom("R" + status.ordinal(), type, status);
            assertThat(roomRepository.findById(room.getId()).orElseThrow().getStatus()).isEqualTo(status);
        }
        assertThat(RoomStatus.values()).hasSize(4);
    }

    // ------------------------------------------------------------------ RF04

    private static String seasonJson(String name, Object start, Object end, String multiplier) {
        return """
                {"name":"%s","startDate":"%s","endDate":"%s","multiplier":%s}
                """.formatted(name, start, end, multiplier);
    }

    @Test
    @DisplayName("RF04: recepção cadastra temporada com período e multiplicador")
    void receptionCreatesSeason() throws Exception {
        String token = receptionToken();

        doPost("/season/register", token, seasonJson("Alta Temporada", day(10), day(20), "1.5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alta Temporada"))
                .andExpect(jsonPath("$.multiplier").value(1.5))
                .andExpect(jsonPath("$.startDate").value(day(10).toString()))
                .andExpect(jsonPath("$.endDate").value(day(20).toString()));
    }

    @Test
    @DisplayName("RF04: temporadas não podem se sobrepor")
    void seasonsCannotOverlap() throws Exception {
        String token = receptionToken();
        doPost("/season/register", token, seasonJson("Alta Temporada", day(10), day(20), "1.5"))
                .andExpect(status().isCreated());

        // sobreposição parcial no fim, no início, contida e envolvendo
        doPost("/season/register", token, seasonJson("A", day(15), day(25), "1.2")).andExpect(status().is4xxClientError());
        doPost("/season/register", token, seasonJson("B", day(5), day(10), "1.2")).andExpect(status().is4xxClientError());
        doPost("/season/register", token, seasonJson("C", day(12), day(14), "1.2")).andExpect(status().is4xxClientError());
        doPost("/season/register", token, seasonJson("D", day(1), day(30), "1.2")).andExpect(status().is4xxClientError());

        assertThat(seasonRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("RF04: temporadas vizinhas, sem dias em comum, são aceitas")
    void adjacentSeasonsAreAccepted() throws Exception {
        String token = receptionToken();
        doPost("/season/register", token, seasonJson("Alta Temporada", day(10), day(20), "1.5"))
                .andExpect(status().isCreated());

        doPost("/season/register", token, seasonJson("Feriado", day(21), day(25), "1.3"))
                .andExpect(status().isCreated());
        doPost("/season/register", token, seasonJson("Baixa", day(1), day(9), "0.8"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RF04: período invertido e multiplicador inválido são rejeitados")
    void seasonValidation() throws Exception {
        String token = receptionToken();

        doPost("/season/register", token, seasonJson("Invertida", day(20), day(10), "1.5"))
                .andExpect(status().is4xxClientError());
        doPost("/season/register", token, seasonJson("Zero", day(10), day(20), "0"))
                .andExpect(status().isBadRequest());
        doPost("/season/register", token, seasonJson("Negativa", day(10), day(20), "-1"))
                .andExpect(status().isBadRequest());
        doPost("/season/register", token, "{\"name\":\"Sem datas\",\"multiplier\":1.5}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF04: nome de temporada repetido é rejeitado")
    void seasonDuplicateName() throws Exception {
        String token = receptionToken();
        doPost("/season/register", token, seasonJson("Alta Temporada", day(10), day(20), "1.5"))
                .andExpect(status().isCreated());

        doPost("/season/register", token, seasonJson("alta temporada", day(30), day(40), "1.5"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("RF04: hóspede não cadastra temporada")
    void guestCannotCreateSeason() throws Exception {
        String guest = guestToken("hospede@email.com");

        doPost("/season/register", guest, seasonJson("Alta Temporada", day(10), day(20), "1.5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF04: a resposta traz o id da temporada criada")
    void seasonResponseHasId() throws Exception {
        String token = receptionToken();

        MvcResult result = doPost("/season/register", token, seasonJson("Alta Temporada", day(10), day(20), "1.5"))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = read(result, "$.id");
        assertThat(seasonRepository.findById(id.longValue())).isPresent();
    }
}
