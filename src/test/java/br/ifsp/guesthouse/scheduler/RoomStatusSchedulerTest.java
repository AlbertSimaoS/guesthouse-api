package br.ifsp.guesthouse.scheduler;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RoomStatusScheduler (unitário)")
class RoomStatusSchedulerTest {

    @Mock private RoomRepository roomRepository;
    @InjectMocks private RoomStatusScheduler scheduler;

    @Test
    @DisplayName("libera quartos em limpeza e em manutenção que passaram do prazo")
    void releasesOverdueRooms() {
        Room cleaning = TestData.room(1, "101", TestData.roomType(1, "S", 2, "10"), RoomStatus.CLEANING);
        Room maintenance = TestData.room(2, "102", TestData.roomType(1, "S", 2, "10"), RoomStatus.MAINTENANCE);
        when(roomRepository.findByStatusAndStatusUpdatedAtBefore(eq(RoomStatus.CLEANING), any()))
                .thenReturn(List.of(cleaning));
        when(roomRepository.findByStatusAndStatusUpdatedAtBefore(eq(RoomStatus.MAINTENANCE), any()))
                .thenReturn(List.of(maintenance));

        scheduler.checkAndTransitionRoomStatuses();

        assertThat(cleaning.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(maintenance.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(roomRepository).save(cleaning);
        verify(roomRepository).save(maintenance);
    }

    @Test
    @DisplayName("sem quartos vencidos não grava nada")
    void nothingToRelease() {
        when(roomRepository.findByStatusAndStatusUpdatedAtBefore(any(), any())).thenReturn(List.of());

        scheduler.checkAndTransitionRoomStatuses();

        verify(roomRepository, never()).save(any());
    }
}
