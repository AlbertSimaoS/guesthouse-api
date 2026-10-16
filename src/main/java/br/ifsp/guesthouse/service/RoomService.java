package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.*;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final SeasonRepository seasonRepository;

    public RoomService(
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository, SeasonRepository seasonRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.seasonRepository = seasonRepository;
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


    @Transactional
    public PageResponseDTO<RoomAvailabilityResponseDTO> findAvailableRooms(
            RoomAvailabilityRequestDTO request,
            Pageable pageable
    ) {
        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();
        Integer guests = request.guests();

        validateAvailabilityRequest(checkIn, checkOut, guests);

        List<ReservationStatus> blockingStatuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED
        );

        List<Room> availableRooms = roomRepository.findAvailableRoomsUnpaged(
                checkIn,
                checkOut,
                guests,
                blockingStatuses
        );

        Map<Long, List<Room>> roomsByType = availableRooms.stream()
                .collect(Collectors.groupingBy(
                        room -> room.getType().getId()
                ));

        List<RoomAvailabilityResponseDTO> results = roomsByType.values()
                .stream()
                .map(rooms -> {
                    RoomType type = rooms.get(0).getType();

                    BigDecimal totalStayPrice = calculateStayPrice(
                            type, checkIn, checkOut
                    );

                    return new RoomAvailabilityResponseDTO(
                            type.getId(),
                            type.getName(),
                            type.getCapacity(),
                            (long) rooms.size(),
                            type.getDailyRate(),
                            totalStayPrice
                    );
                })
                .sorted(Comparator.comparing(
                        RoomAvailabilityResponseDTO::roomTypeName
                ))
                .toList();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = Math.min(page * size, results.size());
        int toIndex = Math.min(fromIndex + size, results.size());

        List<RoomAvailabilityResponseDTO> pageContent =
                results.subList(fromIndex, toIndex);

        Page<RoomAvailabilityResponseDTO> resultPage = new PageImpl<>(
                pageContent,
                pageable,
                results.size()
        );

        return PageResponseDTO.from(resultPage);
    }


    private void validateAvailabilityRequest(
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests
    ) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException(
                    "Check-in e check-out são obrigatórios."
            );
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                    "O check-out deve ser posterior ao check-in."
            );
        }

        if (guests == null || guests <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de hóspedes deve ser maior que zero."
            );
        }
    }

    private BigDecimal calculateStayPrice(
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        BigDecimal basePrice = roomType.getDailyRate();
        BigDecimal total = BigDecimal.ZERO;

        for (LocalDate night = checkIn;
             night.isBefore(checkOut);
             night = night.plusDays(1)) {

            BigDecimal multiplier = seasonRepository
                    .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            night, night
                    )
                    .map(Season::getMultiplier)
                    .orElse(BigDecimal.ONE);

            BigDecimal nightlyPrice = basePrice.multiply(multiplier);

            total = total.add(nightlyPrice);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}