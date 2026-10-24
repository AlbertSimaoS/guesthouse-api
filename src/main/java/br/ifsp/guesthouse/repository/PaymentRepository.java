package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationIdAndStatus(
            Long reservationId,
            PaymentStatus status
    );

    Optional<Payment> findByReservationIdAndType(
            Long reservationId,
            PaymentType type
    );

}
