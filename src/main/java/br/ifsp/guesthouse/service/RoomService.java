package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.RoomRequestDTO;
import br.ifsp.guesthouse.dto.RoomResponseDTO;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomService(
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            SeasonRepository seasonRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public RoomResponseDTO register(RoomRequestDTO request) {

        String number = request.number();

        if (roomRepository.existsByNumberIgnoreCase(number)) {
            throw new IllegalArgumentException(
                    "A room with this number already exists."
            );
        }

        RoomType roomType = roomTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room type not found."
                ));

        Room room = new Room();
        room.setNumber(number);
        room.setType(roomType);

        Room savedRoom = roomRepository.save(room);

        return new RoomResponseDTO(savedRoom);
    }
}