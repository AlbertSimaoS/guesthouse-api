package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationIdAndStatus(
            Long reservationId,
            PaymentStatus status
    );
}
