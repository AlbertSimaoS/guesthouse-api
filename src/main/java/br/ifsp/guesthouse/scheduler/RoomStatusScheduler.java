package br.ifsp.guesthouse.scheduler;

import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.RoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RoomStatusScheduler {

    private final RoomRepository roomRepository;

    public RoomStatusScheduler(RoomRepository roomRepository)
    {
        this.roomRepository = roomRepository;
    }

    @Scheduled(fixedRate = 300_000) // confere a casa 5 minutos no banco
    @Transactional
    public void checkAndTransitionRoomStatuses() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime cleaningLimit = now.minusHours(3);
        List<Room> roomsInCleaning = roomRepository.findByStatusAndStatusUpdatedAtBefore(
                RoomStatus.CLEANING, cleaningLimit
        );

        for (Room room : roomsInCleaning) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        LocalDateTime maintenanceLimit = now.minusDays(2);
        List<Room> roomsInMaintenance = roomRepository.findByStatusAndStatusUpdatedAtBefore(
                RoomStatus.MAINTENANCE, maintenanceLimit
        );

        for (Room room : roomsInMaintenance) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }
    }
}