package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.*;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.*;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.model.Payment;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AvailabilityService availabilityService;
    private final PaymentService paymentService;
    private final RoomRepository roomRepository;
    private final ConsumptionRepository consumptionRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomTypeRepository roomTypeRepository,
            UserAccountRepository userAccountRepository,
            AvailabilityService availabilityService,
            PaymentService paymentService,
            RoomRepository roomRepository,
            ConsumptionRepository consumptionRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userAccountRepository = userAccountRepository;
        this.paymentService = paymentService;
        this.roomRepository = roomRepository;
        this.consumptionRepository = consumptionRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public ReservationResponseDTO createReservation(
            @Valid ReservationRequestDTO request
    ) {
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        Integer numberOfGuests = request.numberOfGuests();
        Integer roomTypeId = request.roomTypeId();

        availabilityService.validatePeriod(startDate, endDate);

        availabilityService.validateHighSeasonMinimumStay(
                startDate,
                endDate
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

        if (roomType.getCapacity() < numberOfGuests) {
            throw new IllegalArgumentException(
                    "The selected room type does not have enough capacity."
            );
        }

        List<AvailabilityResponseDTO> availableRoomsTypes = availabilityService.buildAvailableRoomTypeList(
                startDate,
                endDate,
                numberOfGuests
        );

        AvailabilityResponseDTO selectedRoomType = availableRoomsTypes.stream()
                .filter(room -> room.roomTypeId().equals(roomTypeId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Room type not available for the selected dates."));

        BigDecimal totalPrice = selectedRoomType.totalStayPrice();

        BigDecimal depositAmount = totalPrice
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation reservation = new Reservation();

        reservation.setRoomType(roomType);
        reservation.setRoom(null);
        reservation.setGuest(guest);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setTotalPrice(totalPrice);
        reservation.setDepositAmount(depositAmount);
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation saved = reservationRepository.save(reservation);

        paymentService.createDepositPayment(saved);

        return ReservationResponseDTO.from(saved);
    }

    @Transactional
    public PaymentResponseDTO updateReservationStatus(
            Long paymentId,
            PaymentStatus newStatus) {

        PaymentResponseDTO payment =
                paymentService.updatePaymentStatus(paymentId, newStatus);

        Reservation reservation = reservationRepository
                .findById(payment.reservationId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found."
                        ));

        if (payment.status() == PaymentStatus.CONFIRMED
                && payment.type() == PaymentType.DEPOSIT) {

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new IllegalStateException(
                        "Only pending reservations can be confirmed."
                );
            }

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }

        if (payment.status() == PaymentStatus.CONFIRMED
                && payment.type() == PaymentType.CHECKOUT) {

            if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
                throw new IllegalStateException(
                        "Only checked-in reservations can be finished."
                );
            }

            Room room = reservation.getRoom();

            if (room == null) {
                throw new IllegalStateException(
                        "Reservation has no assigned room."
                );
            }

            if (room.getStatus() != RoomStatus.OCCUPIED) {
                throw new IllegalStateException(
                        "Assigned room is not occupied."
                );
            }

            reservation.setStatus(ReservationStatus.FINISHED);

            room.setStatus(RoomStatus.CLEANING);

            roomRepository.save(room);
            reservationRepository.save(reservation);
        }

        return payment;
    }

    @Transactional
    public int expirePendingReservations() {
        LocalDateTime limit = LocalDateTime.now().minusHours(24);

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

    @Transactional
    public ReservationResponseDTO cancelReservation(Long reservationId) {

        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found."
                        ));

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Only pending or confirmed reservations can be cancelled."
            );
        }

        if (!LocalDate.now().isBefore(reservation.getStartDate())) {
            throw new IllegalStateException(
                    "Reservations cannot be cancelled on or after check-in."
            );
        }

        long daysUntilCheckIn = ChronoUnit.DAYS.between(
                LocalDate.now(),
                reservation.getStartDate()
        );

        boolean eligibleForRefund = daysUntilCheckIn >= 7;

        paymentService.cancelDepositPayment(
                reservation.getId(),
                eligibleForRefund
        );

        reservation.setStatus(ReservationStatus.CANCELLED);

        Reservation saved = reservationRepository.save(reservation);

        return ReservationResponseDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ReservationResponseDTO> getMyReservations(
            Pageable pageable
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalArgumentException(
                    "An authenticated user is required."
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

        Page<ReservationResponseDTO> reservations =
                reservationRepository
                        .findByGuestId(userId, pageable)
                        .map(ReservationResponseDTO::from);

        return PageResponseDTO.from(reservations);
    }

    @Transactional
    public ReservationResponseDTO checkIn(
            Long reservationId,
            Integer roomId
    ) {
        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found."
                        ));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed reservations can check in."
            );
        }

        if (!LocalDate.now().equals(reservation.getStartDate())) {
            throw new IllegalStateException(
                    "Check-in is only allowed on the start date."
            );
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Room not found."
                        ));

        if (room.getType().getId() != reservation.getRoomType().getId()) {
            throw new IllegalArgumentException(
                    "Room does not match the reserved room type."
            );
        }

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Room is not available."
            );
        }

        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        Reservation saved = reservationRepository.save(reservation);

        return ReservationResponseDTO.from(saved);
    }

    @Transactional
    public int processNoShows() {

        LocalDate today = LocalDate.now();

        List<Reservation> reservations =
                reservationRepository.findByStatusAndStartDateBefore(
                        ReservationStatus.CONFIRMED,
                        today
                );

        for (Reservation reservation : reservations) {

            paymentService.retainDepositPayment(reservation.getId());

            reservation.setStatus(ReservationStatus.NO_SHOW);

            reservationRepository.save(reservation);
        }

        return reservations.size();
    }

    @Transactional
    public CheckoutResponseDTO checkOut(Long reservationId) {

        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found."
                        ));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Only checked-in reservations can check out."
            );
        }

        if (reservation.getRoom() == null) {
            throw new IllegalStateException(
                    "Reservation has no assigned room."
            );
        }

        LocalDate checkoutDate = LocalDate.now();

        boolean earlyCheckout =
                checkoutDate.isBefore(reservation.getEndDate());

        BigDecimal stayPrice;
        BigDecimal penalty = BigDecimal.ZERO;

        if (earlyCheckout) {

            LocalDate usedStayEnd = checkoutDate;

            if (!usedStayEnd.isAfter(reservation.getStartDate())) {
                usedStayEnd = reservation.getStartDate().plusDays(1);
            }

            stayPrice = availabilityService.calculateStayPrice(
                    reservation.getRoomType(),
                    reservation.getStartDate(),
                    usedStayEnd
            );

            LocalDate penaltyDate = checkoutDate.plusDays(1);

            penalty = availabilityService.calculateStayPrice(
                    reservation.getRoomType(),
                    penaltyDate,
                    penaltyDate.plusDays(1)
            );

        } else {

            stayPrice = reservation.getTotalPrice();
        }

        BigDecimal consumptionTotal =
                consumptionRepository.sumConsumptionsByReservationId(
                        reservationId
                );

        Payment deposit =
                    paymentService.getConfirmedDeposit(reservationId);

        BigDecimal depositAmount = deposit.getAmount();

        BigDecimal totalDue = stayPrice
                .add(consumptionTotal)
                .add(penalty);

        BigDecimal checkoutAmount =
                totalDue.subtract(depositAmount);

        Payment checkoutPayment =
                paymentService.createCheckoutPayment(
                        reservation,
                        checkoutAmount
                );

        return new CheckoutResponseDTO(
                reservation.getId(),
                checkoutPayment.getId(),
                checkoutDate,
                earlyCheckout,
                stayPrice,
                consumptionTotal,
                penalty,
                depositAmount,
                totalDue,
                checkoutAmount,
                checkoutPayment.getStatus()
        );
    }
}