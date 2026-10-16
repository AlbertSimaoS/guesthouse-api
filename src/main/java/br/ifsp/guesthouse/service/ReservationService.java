package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.ReservationRequestDTO;
import br.ifsp.guesthouse.dto.ReservationResponseDTO;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;
import br.ifsp.guesthouse.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserAccountRepository userAccountRepository;
    private final SeasonRepository seasonRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            UserAccountRepository userAccountRepository,
            SeasonRepository seasonRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userAccountRepository = userAccountRepository;
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public ReservationResponseDTO createReservation(
            ReservationRequestDTO request
    ) {
        Long userId = getAuthenticatedUserId();

        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();

        if (checkIn == null || checkOut == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-in and check-out dates are required."
            );
        }

        if (!checkIn.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-in must be in the future."
            );
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Check-out must be after check-in."
            );
        }

        if (request.roomId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room ID is required."
            );
        }

        if (request.numberOfGuests() == null
                || request.numberOfGuests() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Number of guests must be positive."
            );
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found."
                ));

        if (request.numberOfGuests() > room.getType().getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The room capacity is insufficient."
            );
        }

        List<ReservationStatus> blockingStatuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED
        );

        boolean hasConflict =
                reservationRepository.existsOverlappingReservation(
                        room.getId(),
                        checkIn,
                        checkOut,
                        blockingStatuses
                );

        if (hasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The room is not available for the selected dates."
            );
        }

        UserAccount guest = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));

        RoomType roomType = room.getType();

        BigDecimal totalPrice = calculateStayPrice(
                roomType,
                checkIn,
                checkOut
        );

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setGuest(guest);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setNumberOfGuests(request.numberOfGuests());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation savedReservation =
                reservationRepository.save(reservation);

        return new ReservationResponseDTO(
                savedReservation.getId(),
                savedReservation.getRoom().getId(),
                savedReservation.getGuest().getId(),
                savedReservation.getCheckIn(),
                savedReservation.getCheckOut(),
                savedReservation.getNumberOfGuests(),
                savedReservation.getTotalPrice(),
                savedReservation.getStatus()
        );
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User authentication is required."
            );
        }

        return userId;
    }

    private BigDecimal calculateStayPrice(
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal dailyRate = roomType.getDailyRate();

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
}