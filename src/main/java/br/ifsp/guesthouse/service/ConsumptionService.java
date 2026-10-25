package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.ConsumptionRequestDTO;
import br.ifsp.guesthouse.dto.ConsumptionResponseDTO;
import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Consumption;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.ConsumptionRepository;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.service.PaymentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsumptionService {

    private final ConsumptionRepository consumptionRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;

    public ConsumptionService(
            ConsumptionRepository consumptionRepository,
            ReservationRepository reservationRepository,
            PaymentService paymentService
    ) {
        this.consumptionRepository = consumptionRepository;
        this.reservationRepository = reservationRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public ConsumptionResponseDTO createConsumption(
            Long reservationId,
            ConsumptionRequestDTO request
    ) {

        Reservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found."
                        )
                );

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Consumptions can only be registered during an active stay."
            );
        }

        if (paymentService.checkoutPaymentExists(reservationId)) {
            throw new IllegalStateException(
                    "Cannot register consumptions after checkout initiation."
            );
        }

        Consumption consumption = new Consumption();

        consumption.setReservation(reservation);
        consumption.setType(request.type());
        consumption.setDescription(request.description());
        consumption.setAmount(request.amount());

        Consumption saved = consumptionRepository.save(consumption);

        return ConsumptionResponseDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ConsumptionResponseDTO> getConsumptionsByReservation(
            Long reservationId,
            Pageable pageable
    ) {

        if (!reservationRepository.existsById(reservationId)) {
            throw new ResourceNotFoundException(
                    "Reservation not found."
            );
        }

        Page<ConsumptionResponseDTO> consumptions =
                consumptionRepository
                        .findByReservationId(reservationId, pageable)
                        .map(ConsumptionResponseDTO::from);

        return PageResponseDTO.from(consumptions);
    }
}