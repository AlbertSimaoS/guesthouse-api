package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.RoomTypeRequestDTO;
import br.ifsp.guesthouse.dto.RoomTypeResponseDTO;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.repository.RoomTypeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public RoomTypeResponseDTO register(RoomTypeRequestDTO request) {
        String name = request.name().trim();

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Room type name is required."
            );
        }

        if (roomTypeRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A room type with this name already exists."
            );
        }

        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setCapacity(request.capacity());
        roomType.setDailyRate(request.dailyRate());

        RoomType savedRoomType = roomTypeRepository.save(roomType);

        return RoomTypeResponseDTO.fromEntity(savedRoomType);
    }
}