package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.ReservationRequestDTO;
import br.ifsp.guesthouse.dto.ReservationResponseDTO;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import br.ifsp.guesthouse.dto.PaymentResponseDTO;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AvailabilityService availabilityService;
    private final PaymentService paymentService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomTypeRepository roomTypeRepository,
            UserAccountRepository userAccountRepository,
            AvailabilityService availabilityService,
            PaymentService paymentService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userAccountRepository = userAccountRepository;
        this.availabilityService = availabilityService;
        this.paymentService = paymentService;
    }

    @Transactional
    public ReservationResponseDTO createReservation(
            @Valid ReservationRequestDTO request
    ) {
        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();
        Integer guests = request.numberOfGuests();
        Integer roomTypeId = request.roomTypeId();

        availabilityService.validatePeriod(checkIn, checkOut);

        if (guests == null || guests <= 0) {
            throw new IllegalArgumentException(
                    "The number of guests must be positive."
            );
        }

        if (roomTypeId == null) {
            throw new IllegalArgumentException(
                    "Room type is required."
            );
        }

        availabilityService.validateHighSeasonMinimumStay(
                checkIn,
                checkOut
        );

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalArgumentException(
                    "An authenticated user is required to make a reservation."
            );
        }

        Long userId;

        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid authenticated user ID."
            );
        }

        UserAccount guest = userAccountRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found."
                        )
                );

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room type not found."
                        )
                );

        if (roomType.getCapacity() < guests) {
            throw new IllegalArgumentException(
                    "The selected room type does not have enough capacity."
            );
        }

        List<Room> availableRooms =
                availabilityService.findAvailableRooms(
                        roomTypeId,
                        guests,
                        checkIn,
                        checkOut
                );

        Room room = availableRooms.stream()
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No available room found for the selected type and period."
                        )
                );

        BigDecimal totalPrice =
                availabilityService.calculateStayPrice(
                        roomType,
                        checkIn,
                        checkOut
                );

        BigDecimal depositAmount = totalPrice
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation reservation = new Reservation();

        reservation.setRoom(room);
        reservation.setGuest(guest);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setNumberOfGuests(guests);
        reservation.setTotalPrice(totalPrice);
        reservation.setDepositAmount(depositAmount);
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation saved = reservationRepository.save(reservation);

        paymentService.createDepositPayment(saved);

        return new ReservationResponseDTO(
                saved.getId(),
                saved.getRoom().getId(),
                saved.getGuest().getId(),
                saved.getCheckIn(),
                saved.getCheckOut(),
                saved.getNumberOfGuests(),
                saved.getTotalPrice(),
                saved.getDepositAmount(),
                saved.getStatus()
        );
    }

    @Transactional
    public PaymentResponseDTO updatePaymentStatus(
            Long paymentId,
            PaymentStatus newStatus) {

        PaymentResponseDTO payment =
                paymentService.updatePaymentStatus(paymentId, newStatus);

        if (payment.status() == PaymentStatus.CONFIRMED
                && payment.type() == PaymentType.DEPOSIT) {

            Reservation reservation = reservationRepository
                    .findById(payment.reservationId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Reservation not found."
                            ));

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }

        return payment;
    }

    @Transactional
    public int expirePendingReservations() {
        LocalDateTime limit = LocalDateTime.now().minusHours(0); //alterar para 24

        List<Reservation> reservations =
                reservationRepository.findByStatusAndCreatedAtBefore(
                        ReservationStatus.PENDING,
                        limit
                );

        for (Reservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            paymentService.expirePendingPayments(reservation.getId());
            reservationRepository.save(reservation);
        }
        return reservations.size();
    }
}