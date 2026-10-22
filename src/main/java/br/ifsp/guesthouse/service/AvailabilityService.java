package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.dto.AvailabilityRequestDTO;
import br.ifsp.guesthouse.dto.RoomAvailabilityResponseDTO;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

	private final RoomRepository roomRepository;
	private final SeasonRepository seasonRepository;

	public AvailabilityService(
			RoomRepository roomRepository,
			SeasonRepository seasonRepository
	) {
		this.roomRepository = roomRepository;
		this.seasonRepository = seasonRepository;
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<RoomAvailabilityResponseDTO> findAvailableRooms(
			AvailabilityRequestDTO request,
			Pageable pageable
	) {
		LocalDate checkIn = request.checkIn();
		LocalDate checkOut = request.checkOut();
		Integer guests = request.guests();

		validatePeriod(checkIn, checkOut);

		List<RoomAvailabilityResponseDTO> availableRooms =
				availableRoomsList(null, checkIn, checkOut, guests);

		return paginateAvailableRooms(availableRooms, pageable);
	}

	@Transactional(readOnly = true)
	public List<Room> findAvailableRooms(
			Integer roomTypeId,
			Integer guests,
			LocalDate checkIn,
			LocalDate checkOut
	) {
		validatePeriod(checkIn, checkOut);

		if (guests == null || guests <= 0) {
			throw new IllegalArgumentException(
					"The number of guests must be positive."
			);
		}

		List<ReservationStatus> blockingStatuses = List.of(
				ReservationStatus.PENDING,
				ReservationStatus.CONFIRMED
		);

		List<Room> availableRooms =
				roomRepository.findAvailableRoomsUnpaged(
						checkIn,
						checkOut,
						guests,
						blockingStatuses
				);

		if (roomTypeId == null) {
			return availableRooms;
		}

		return availableRooms.stream()
				.filter(room ->
						room.getType().getId() == roomTypeId
				)
				.toList();
	}

	private List<RoomAvailabilityResponseDTO> availableRoomsList(
			Integer roomTypeId,
			LocalDate checkIn,
			LocalDate checkOut,
			Integer guests
	) {
		List<Room> availableRooms = findAvailableRooms(
				roomTypeId,
				guests,
				checkIn,
				checkOut
		);

		Map<Integer, List<Room>> roomsByType =
				availableRooms.stream()
						.collect(Collectors.groupingBy(
								room -> room.getType().getId()
						));

		return roomsByType.values().stream()
				.map(rooms -> {
					RoomType type = rooms.get(0).getType();

					Integer availableRoomsCount = rooms.size();

					BigDecimal totalStayPrice = calculateStayPrice(
							type,
							checkIn,
							checkOut
					);

					return new RoomAvailabilityResponseDTO(
							type.getId(),
							type.getName(),
							type.getCapacity(),
							availableRoomsCount,
							type.getDailyRate(),
							totalStayPrice
					);
				})
				.sorted(Comparator.comparing(
						RoomAvailabilityResponseDTO::roomTypeName
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public BigDecimal calculateStayPrice(
			RoomType roomType,
			LocalDate checkIn,
			LocalDate checkOut
	) {
		validatePeriod(checkIn, checkOut);

		BigDecimal dailyRate = roomType.getDailyRate();
		BigDecimal total = BigDecimal.ZERO;

		for (LocalDate night = checkIn;
			 night.isBefore(checkOut);
			 night = night.plusDays(1)) {

			BigDecimal multiplier = seasonRepository
					.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
							night,
							night
					)
					.map(Season::getMultiplier)
					.orElse(BigDecimal.ONE);

			total = total.add(dailyRate.multiply(multiplier));
		}

		return total.setScale(2, RoundingMode.HALF_UP);
	}

	@Transactional(readOnly = true)
	public void validateHighSeasonMinimumStay(
			LocalDate checkIn,
			LocalDate checkOut
	) {
		validatePeriod(checkIn, checkOut);

		long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

		boolean includesHighSeason = seasonRepository
				.findAllByStartDateLessThanAndEndDateGreaterThan(
						checkOut,
						checkIn
				)
				.stream()
				.anyMatch(season ->
						"Alta Temporada".equalsIgnoreCase(
								season.getName()
						)
				);

		if (includesHighSeason && nights < 2) {
			throw new IllegalArgumentException(
					"Reservations during Alta Temporada must be at least 2 nights."
			);
		}
	}

	private PageResponseDTO<RoomAvailabilityResponseDTO> paginateAvailableRooms(
			List<RoomAvailabilityResponseDTO> availableRooms,
			Pageable pageable
	) {
		int page = pageable.getPageNumber();
		int size = pageable.getPageSize();

		int fromIndex = (int) Math.min(
				(long) page * size,
				availableRooms.size()
		);

		int toIndex = (int) Math.min(
				(long) fromIndex + size,
				availableRooms.size()
		);

		List<RoomAvailabilityResponseDTO> pageContent =
				availableRooms.subList(fromIndex, toIndex);

		Page<RoomAvailabilityResponseDTO> resultPage = new PageImpl<>(
				pageContent,
				pageable,
				availableRooms.size()
		);

		return PageResponseDTO.from(resultPage);
	}

	public void validatePeriod(
			LocalDate checkIn,
			LocalDate checkOut
	) {
		if (checkIn == null || checkOut == null) {
			throw new IllegalArgumentException(
					"Check-in and check-out dates are required."
			);
		}

		if (!checkOut.isAfter(checkIn)) {
			throw new IllegalArgumentException(
					"Check-out must be after check-in."
			);
		}
	}
}