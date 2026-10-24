
package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.PaymentResponseDTO;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void createDepositPayment(Reservation reservation) {
        Payment payment = new Payment();

        payment.setReservation(reservation);
        payment.setType(PaymentType.DEPOSIT);
        payment.setAmount(reservation.getDepositAmount());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResponseDTO updatePaymentStatus(
            Long paymentId,
            PaymentStatus newStatus) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found."));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be updated.");
        }

        if (newStatus != PaymentStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Status must be CONFIRMED.");
        }

        payment.setStatus(newStatus);

        Payment saved = paymentRepository.save(payment);

        return new PaymentResponseDTO(
                saved.getId(),
                saved.getReservation().getId(),
                saved.getType(),
                saved.getAmount(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }
}
