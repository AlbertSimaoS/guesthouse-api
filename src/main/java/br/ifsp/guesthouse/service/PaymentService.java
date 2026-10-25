
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

import java.math.BigDecimal;
import java.util.List;

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

    @Transactional
    public void expirePendingPayments(Long reservationId) {

        List<Payment> payments =
                paymentRepository.findByReservationIdAndStatus(
                        reservationId,
                        PaymentStatus.PENDING
                );

        for (Payment payment : payments) {
            payment.setStatus(PaymentStatus.EXPIRED);
        }

        paymentRepository.saveAll(payments);
    }

    @Transactional
    public void cancelDepositPayment(
            Long reservationId,
            boolean eligibleForRefund
    ) {
        Payment payment = paymentRepository
                .findByReservationIdAndType(
                        reservationId,
                        PaymentType.DEPOSIT
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Deposit payment not found."
                        ));

        if (payment.getStatus() == PaymentStatus.PENDING) {

            payment.setStatus(PaymentStatus.CANCELLED);

        } else if (payment.getStatus() == PaymentStatus.CONFIRMED) {

            if (eligibleForRefund) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.RETAINED);
            }

        } else {
            throw new IllegalStateException(
                    "Deposit payment cannot be cancelled."
            );
        }

        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getConfirmedDeposit(Long reservationId) {

        Payment deposit = paymentRepository
                .findByReservationIdAndType(
                        reservationId,
                        PaymentType.DEPOSIT
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Deposit payment not found."
                        ));

        if (deposit.getStatus() != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Deposit payment is not confirmed."
            );
        }

        return deposit;
    }

    @Transactional
    public void retainDepositPayment(Long reservationId) {

        Payment payment = paymentRepository
                .findByReservationIdAndType(
                        reservationId,
                        PaymentType.DEPOSIT
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Deposit payment not found."
                        ));

        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed deposits can be retained."
            );
        }

        payment.setStatus(PaymentStatus.RETAINED);

        paymentRepository.save(payment);
    }

    @Transactional
    public Payment createCheckoutPayment(
            Reservation reservation,
            BigDecimal amount
    ) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Checkout payment amount must be positive."
            );
        }

        Payment payment = new Payment();

        payment.setReservation(reservation);
        payment.setType(PaymentType.CHECKOUT);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public boolean checkoutPaymentExists(Long reservationId) {
        return paymentRepository.existsByReservationIdAndType(
                reservationId,
                PaymentType.CHECKOUT
        );
    }
}
