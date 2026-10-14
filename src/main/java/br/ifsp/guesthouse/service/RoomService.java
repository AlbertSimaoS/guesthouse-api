package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.RoomRequestDTO;
import br.ifsp.guesthouse.dto.RoomResponseDTO;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomService(
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public RoomResponseDTO register(RoomRequestDTO request) {
        String number = request.number();

        if (roomRepository.existsByNumberIgnoreCase(number)) {
            throw new IllegalArgumentException(
                    "Já existe um quarto com esse número"
            );
        }

        Long typeId = request.typeId();

        RoomType roomType = roomTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Esse tipo de quarto não existe"
                ));

        Room room = new Room();
        room.setNumber(number);
        room.setType(roomType);

        Room savedRoom = roomRepository.save(room);

        return new RoomResponseDTO(savedRoom);
    }
}