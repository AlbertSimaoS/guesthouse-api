package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.AvailabilityRequestDTO;
import br.ifsp.guesthouse.dto.AvailabilityResponseDTO;
import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AvailabilityService (unitário) - RF05/RF06")
class AvailabilityServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private ReservationRepository reservationRepository;

    private AvailabilityService service;

    private final LocalDate d0 = LocalDate.of(2026, 12, 10);

    @BeforeEach
    void setUp() {
        service = new AvailabilityService(roomRepository, seasonRepository, roomTypeRepository, reservationRepository);
    }

    // ------------------------------------------------------------ período

    @Test
    @DisplayName("validatePeriod: datas nulas, iguais ou invertidas são inválidas; saída depois da entrada é válida")
    void validatePeriod() {
        assertThatThrownBy(() -> service.validatePeriod(null, d0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validatePeriod(d0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validatePeriod(d0, d0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validatePeriod(d0.plusDays(1), d0)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> service.validatePeriod(d0, d0.plusDays(1))).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------- preço

    @Test
    @DisplayName("calculateStayPrice: soma noite a noite; só a noite em temporada recebe o multiplicador")
    void priceNightByNight() {
        RoomType type = TestData.roomType(1, "Standard", 2, "100.00");
        LocalDate seasonNight = d0.plusDays(1);
        when(seasonRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(eq(seasonNight), eq(seasonNight)))
                .thenReturn(Optional.of(TestData.season("Alta Temporada", seasonNight, seasonNight, "1.5")));

        // 3 noites: 100 + 150 + 100
        assertThat(service.calculateStayPrice(type, d0, d0.plusDays(3))).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("calculateStayPrice: sem temporada vale a diária base; a saída não é cobrada")
    void priceWithoutSeason() {
        RoomType type = TestData.roomType(1, "Standard", 2, "80.00");

        assertThat(service.calculateStayPrice(type, d0, d0.plusDays(1))).isEqualByComparingTo("80.00");
        assertThat(service.calculateStayPrice(type, d0, d0.plusDays(5))).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("calculateStayPrice: arredonda para 2 casas")
    void priceIsRounded() {
        RoomType type = TestData.roomType(1, "Standard", 2, "33.33");
        when(seasonRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(Optional.of(TestData.season("Qualquer", d0, d0.plusDays(5), "1.555")));

        // 33,33 x 1,555 = 51,82815 -> 51,83
        assertThat(service.calculateStayPrice(type, d0, d0.plusDays(1))).isEqualByComparingTo("51.83");
    }

    @Test
    @DisplayName("calculateStayPrice: período inválido lança exceção")
    void priceInvalidPeriod() {
        RoomType type = TestData.roomType(1, "Standard", 2, "100.00");

        assertThatThrownBy(() -> service.calculateStayPrice(type, d0, d0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------ estadia mínima (RF06)

    @Test
    @DisplayName("alta temporada: 1 noite é recusada, 2 noites são aceitas")
    void highSeasonMinimumStay() {
        when(seasonRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(Optional.of(TestData.season("Alta Temporada", d0, d0.plusDays(10), "1.5")));

        assertThatThrownBy(() -> service.validateHighSeasonMinimumStay(d0, d0.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> service.validateHighSeasonMinimumStay(d0, d0.plusDays(2))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o nome da temporada é comparado sem diferenciar maiúsculas")
    void highSeasonNameIsCaseInsensitive() {
        when(seasonRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(Optional.of(TestData.season("ALTA TEMPORADA", d0, d0.plusDays(10), "1.5")));

        assertThatThrownBy(() -> service.validateHighSeasonMinimumStay(d0, d0.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("outras temporadas e ausência de temporada não exigem estadia mínima")
    void otherSeasonsHaveNoMinimumStay() {
        when(seasonRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(Optional.of(TestData.season("Feriado", d0, d0.plusDays(10), "1.2")));
        assertThatCode(() -> service.validateHighSeasonMinimumStay(d0, d0.plusDays(1))).doesNotThrowAnyException();

        when(seasonRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(Optional.empty());
        assertThatCode(() -> service.validateHighSeasonMinimumStay(d0, d0.plusDays(1))).doesNotThrowAnyException();
    }

    // ------------------------------------------------------ disponibilidade

    @Test
    @DisplayName("lista os tipos disponíveis com quartos livres e valor da estadia, ordenados por nome")
    void listsAvailableTypes() {
        RoomType suite = TestData.roomType(2, "Suite", 3, "300.00");
        RoomType standard = TestData.roomType(1, "Standard", 2, "100.00");
        when(roomTypeRepository.findAvailableRoomTypes(any(), any(), any(), any()))
                .thenReturn(List.of(suite, standard));
        when(roomRepository.countByType(standard)).thenReturn(3);
        when(roomRepository.countByType(suite)).thenReturn(1);
        when(reservationRepository.countBlockingReservations(eq(1), any(), any(), any())).thenReturn(1L);
        when(reservationRepository.countBlockingReservations(eq(2), any(), any(), any())).thenReturn(0L);

        List<AvailabilityResponseDTO> result =
                service.availableRoomTypes(new AvailabilityRequestDTO(d0, d0.plusDays(2), 2));

        assertThat(result).extracting(AvailabilityResponseDTO::roomTypeName).containsExactly("Standard", "Suite");
        assertThat(result.get(0).availableRooms()).isEqualTo(2);
        assertThat(result.get(0).totalStayPrice()).isEqualByComparingTo("200.00");
        assertThat(result.get(1).availableRooms()).isEqualTo(1);
        assertThat(result.get(1).totalStayPrice()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("período inválido não chega a consultar os repositórios")
    void invalidPeriodOnAvailability() {
        assertThatThrownBy(() -> service.availableRoomTypes(new AvailabilityRequestDTO(d0, d0, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------- paginação

    private List<AvailabilityResponseDTO> items(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new AvailabilityResponseDTO(i, "Tipo " + i, 2, 1, BigDecimal.TEN, BigDecimal.TEN))
                .toList();
    }

    @Test
    @DisplayName("paginateAvailableRoomTypes: devolve a página pedida e os totais")
    void paginates() {
        PageResponseDTO<AvailabilityResponseDTO> page = service.paginateAvailableRoomTypes(items(5), PageRequest.of(1, 2));

        assertThat(page.content()).extracting(AvailabilityResponseDTO::roomTypeId).containsExactly(3, 4);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("paginateAvailableRoomTypes: última página parcial e página além do fim")
    void paginatesEdges() {
        assertThat(service.paginateAvailableRoomTypes(items(5), PageRequest.of(2, 2)).content()).hasSize(1);
        assertThat(service.paginateAvailableRoomTypes(items(5), PageRequest.of(9, 2)).content()).isEmpty();
        assertThat(service.paginateAvailableRoomTypes(List.of(), PageRequest.of(0, 10)).content()).isEmpty();
    }
}
