package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.dto.AvailabilityRequestDTO;
import br.ifsp.guesthouse.dto.AvailabilityResponseDTO;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
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
import java.util.Objects;

@Service
public class AvailabilityService {

	private final RoomRepository roomRepository;
	private final SeasonRepository seasonRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final ReservationRepository reservationRepository;

	public AvailabilityService(
			RoomRepository roomRepository,
			SeasonRepository seasonRepository, RoomTypeRepository roomTypeRepository, ReservationRepository reservationRepository
	) {
		this.roomRepository = roomRepository;
		this.seasonRepository = seasonRepository;
		this.roomTypeRepository = roomTypeRepository;
		this.reservationRepository = reservationRepository;
	}


	@Transactional(readOnly = true)
	public List<AvailabilityResponseDTO> availableRoomTypes(
			AvailabilityRequestDTO request
	) {
		LocalDate startDate = request.startDate();
		LocalDate endDate = request.endDate();
		Integer numberOfGuests = request.numberOfGuests();

		validatePeriod(startDate, endDate);

		return buildAvailableRoomTypeList(
				startDate,
				endDate,
				numberOfGuests
		);
	}


	List<AvailabilityResponseDTO> buildAvailableRoomTypeList(
			LocalDate startDate,
			LocalDate endDate,
			Integer numberOfguests
	) {
		List<ReservationStatus> blockingStatuses = List.of(
				ReservationStatus.CONFIRMED,
				ReservationStatus.CHECKED_IN,
				ReservationStatus.PENDING
		);

		return roomTypeRepository.findByCapacityGreaterThanEqual(numberOfguests)
				.stream()
				.map(type -> {
					// quartos em manutenção não contam na disponibilidade
					int usableRooms = roomRepository.countByTypeAndStatusNot(
							type,
							RoomStatus.MAINTENANCE
					);

					List<Reservation> overlapping =
							reservationRepository.findBlockingReservations(
									type.getId(),
									startDate,
									endDate,
									blockingStatuses
							);

					// a disponibilidade é avaliada noite a noite: vale o pior caso
					int peakOccupancy = 0;
					for (LocalDate night = startDate;
						 night.isBefore(endDate);
						 night = night.plusDays(1)) {

						final LocalDate currentNight = night;
						int occupied = (int) overlapping.stream()
								.filter(r -> !r.getStartDate().isAfter(currentNight)
										&& r.getEndDate().isAfter(currentNight))
								.count();

						peakOccupancy = Math.max(peakOccupancy, occupied);
					}

					int availableRooms = usableRooms - peakOccupancy;

					if (availableRooms <= 0) {
						return null;
					}

					BigDecimal totalStayPrice = calculateStayPrice(
							type,
							startDate,
							endDate
					);

					return AvailabilityResponseDTO.from(type, availableRooms, totalStayPrice);
				})
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(
						AvailabilityResponseDTO::roomTypeName
				))
				.toList();
	}

	public PageResponseDTO<AvailabilityResponseDTO>
	paginateAvailableRoomTypes(
			List<AvailabilityResponseDTO> availableRoomTypes,
			Pageable pageable
	) {
		int page = pageable.getPageNumber();
		int size = pageable.getPageSize();

		int fromIndex = (int) Math.min(
				(long) page * size,
				availableRoomTypes.size()
		);

		int toIndex = (int) Math.min(
				(long) fromIndex + size,
				availableRoomTypes.size()
		);

		List<AvailabilityResponseDTO> pageContent =
				availableRoomTypes.subList(fromIndex, toIndex);

		Page<AvailabilityResponseDTO> resultPage = new PageImpl<>(
				pageContent,
				pageable,
				availableRoomTypes.size()
		);

		return PageResponseDTO.from(resultPage);
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

		// a saída não é uma noite
		boolean includesHighSeason = seasonRepository
				.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDate(
						checkOut.minusDays(1),
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