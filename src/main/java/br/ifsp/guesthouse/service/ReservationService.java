package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.AvailabilityResponseDTO;
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
}