package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.RoomRequestDTO;
import br.ifsp.guesthouse.dto.RoomResponseDTO;
import br.ifsp.guesthouse.dto.RoomTypeRequestDTO;
import br.ifsp.guesthouse.dto.RoomTypeResponseDTO;
import br.ifsp.guesthouse.dto.SeasonRequestDTO;
import br.ifsp.guesthouse.dto.SeasonResponseDTO;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RF03 e RF04: RoomTypeService, RoomService e SeasonService. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Cadastros (unitário) - RF03/RF04")
class CatalogServicesTest {

    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private SeasonRepository seasonRepository;

    // --------------------------------------------------------------- RoomType

    @Nested
    @DisplayName("RoomTypeService")
    class RoomTypes {

        private RoomTypeService service() {
            return new RoomTypeService(roomTypeRepository);
        }

        @Test
        @DisplayName("cadastra tipo de quarto")
        void registers() {
            when(roomTypeRepository.existsByNameIgnoreCase("Duplo")).thenReturn(false);
            when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(inv -> inv.getArgument(0));

            RoomTypeResponseDTO response = service()
                    .register(new RoomTypeRequestDTO("  Duplo ", 2, new BigDecimal("350.00")));

            assertThat(response.name()).isEqualTo("Duplo");
            assertThat(response.capacity()).isEqualTo(2);
            assertThat(response.dailyRate()).isEqualByComparingTo("350.00");
        }

        @Test
        @DisplayName("nome duplicado é rejeitado")
        void duplicateName() {
            when(roomTypeRepository.existsByNameIgnoreCase("Duplo")).thenReturn(true);

            assertThatThrownBy(() -> service().register(new RoomTypeRequestDTO("Duplo", 2, BigDecimal.TEN)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(roomTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("nome em branco é rejeitado")
        void blankName() {
            assertThatThrownBy(() -> service().register(new RoomTypeRequestDTO("   ", 2, BigDecimal.TEN)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ------------------------------------------------------------------- Room

    @Nested
    @DisplayName("RoomService")
    class Rooms {

        private RoomService service() {
            return new RoomService(roomRepository, roomTypeRepository, seasonRepository);
        }

        @Test
        @DisplayName("cadastra quarto livre associado ao tipo")
        void registers() {
            RoomType type = TestData.roomType(1, "Standard", 2, "100.00");
            when(roomRepository.existsByNumberIgnoreCase("101")).thenReturn(false);
            when(roomTypeRepository.findById(1)).thenReturn(Optional.of(type));
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

            RoomResponseDTO response = service().register(new RoomRequestDTO("101", 1));

            assertThat(response.number()).isEqualTo("101");
            assertThat(response.status()).isEqualTo(RoomStatus.AVAILABLE);
            assertThat(response.typeId()).isEqualTo(1);
            assertThat(response.typeName()).isEqualTo("Standard");
        }

        @Test
        @DisplayName("número duplicado e tipo inexistente são rejeitados")
        void duplicateNumberAndUnknownType() {
            when(roomRepository.existsByNumberIgnoreCase("101")).thenReturn(true);
            assertThatThrownBy(() -> service().register(new RoomRequestDTO("101", 1)))
                    .isInstanceOf(IllegalArgumentException.class);

            when(roomRepository.existsByNumberIgnoreCase("102")).thenReturn(false);
            when(roomTypeRepository.findById(9)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service().register(new RoomRequestDTO("102", 9)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("libera o quarto depois da limpeza e registra o instante da mudança")
        void releasesAfterCleaning() {
            Room room = TestData.room(5, "101", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.CLEANING);
            when(roomRepository.findById(5)).thenReturn(Optional.of(room));

            service().updateRoomStatus(5, RoomStatus.AVAILABLE);

            assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
            assertThat(room.getStatusUpdatedAt()).isNotNull();
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("quarto inexistente, mesmo status e quarto ocupado são rejeitados")
        void invalidTransitions() {
            Room occupied = TestData.room(6, "102", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.OCCUPIED);
            Room available = TestData.room(7, "103", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.AVAILABLE);
            when(roomRepository.findById(6)).thenReturn(Optional.of(occupied));
            when(roomRepository.findById(7)).thenReturn(Optional.of(available));
            when(roomRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().updateRoomStatus(99, RoomStatus.AVAILABLE))
                    .isInstanceOf(EntityNotFoundException.class);
            assertThatThrownBy(() -> service().updateRoomStatus(7, RoomStatus.AVAILABLE))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service().updateRoomStatus(6, RoomStatus.MAINTENANCE))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(occupied.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("manutenção: aceita a partir de quarto livre ou em limpeza")
        void maintenanceFromAvailableOrCleaning() {
            Room available = TestData.room(7, "103", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.AVAILABLE);
            Room cleaning = TestData.room(8, "104", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.CLEANING);
            when(roomRepository.findById(7)).thenReturn(Optional.of(available));
            when(roomRepository.findById(8)).thenReturn(Optional.of(cleaning));

            service().updateRoomStatus(7, RoomStatus.MAINTENANCE);
            service().updateRoomStatus(8, RoomStatus.MAINTENANCE);

            assertThat(available.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
            assertThat(cleaning.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("quarto em manutenção pode ser liberado")
        void maintenanceToAvailable() {
            Room room = TestData.room(9, "105", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.MAINTENANCE);
            when(roomRepository.findById(9)).thenReturn(Optional.of(room));

            service().updateRoomStatus(9, RoomStatus.AVAILABLE);

            assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }

        @Test
        @DisplayName("ocupado e limpeza não podem ser definidos manualmente")
        void occupiedAndCleaningAreNotManual() {
            Room available = TestData.room(7, "103", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.AVAILABLE);
            Room maintenance = TestData.room(9, "105", TestData.roomType(1, "Standard", 2, "100"), RoomStatus.MAINTENANCE);
            when(roomRepository.findById(7)).thenReturn(Optional.of(available));
            when(roomRepository.findById(9)).thenReturn(Optional.of(maintenance));

            assertThatThrownBy(() -> service().updateRoomStatus(7, RoomStatus.OCCUPIED))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service().updateRoomStatus(9, RoomStatus.CLEANING))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(available.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
            assertThat(maintenance.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
            verify(roomRepository, never()).save(any());
        }
    }

    // ----------------------------------------------------------------- Season

    @Nested
    @DisplayName("SeasonService")
    class Seasons {

        private final LocalDate start = LocalDate.of(2026, 12, 20);
        private final LocalDate end = LocalDate.of(2027, 1, 10);

        private SeasonService service() {
            return new SeasonService(seasonRepository);
        }

        @Test
        @DisplayName("cadastra temporada")
        void registers() {
            when(seasonRepository.save(any(Season.class))).thenAnswer(inv -> inv.getArgument(0));

            SeasonResponseDTO response = service()
                    .register(new SeasonRequestDTO(" Alta Temporada ", start, end, new BigDecimal("1.5")));

            assertThat(response.name()).isEqualTo("Alta Temporada");
            assertThat(response.startDate()).isEqualTo(start);
            assertThat(response.endDate()).isEqualTo(end);
            assertThat(response.multiplier()).isEqualByComparingTo("1.5");
        }

        @Test
        @DisplayName("nome repetido, período invertido, sobreposição e multiplicador <= 0 são rejeitados")
        void invalidSeasons() {
            when(seasonRepository.existsByNameIgnoreCase("Repetida")).thenReturn(true);
            assertThatThrownBy(() -> service().register(new SeasonRequestDTO("Repetida", start, end, BigDecimal.ONE)))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> service().register(new SeasonRequestDTO("Invertida", end, start, BigDecimal.ONE)))
                    .isInstanceOf(IllegalArgumentException.class);

            when(seasonRepository.existsOverlappingSeason(start, end)).thenReturn(true);
            assertThatThrownBy(() -> service().register(new SeasonRequestDTO("Sobreposta", start, end, BigDecimal.ONE)))
                    .isInstanceOf(IllegalArgumentException.class);

            when(seasonRepository.existsOverlappingSeason(start, end)).thenReturn(false);
            assertThatThrownBy(() -> service().register(new SeasonRequestDTO("Zero", start, end, BigDecimal.ZERO)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(seasonRepository, never()).save(any());
        }

        @Test
        @DisplayName("temporada de um único dia é válida")
        void singleDaySeason() {
            when(seasonRepository.save(any(Season.class))).thenAnswer(inv -> inv.getArgument(0));

            SeasonResponseDTO response = service()
                    .register(new SeasonRequestDTO("Feriado", start, start, new BigDecimal("2.0")));

            assertThat(response.startDate()).isEqualTo(response.endDate());
        }
    }
}
