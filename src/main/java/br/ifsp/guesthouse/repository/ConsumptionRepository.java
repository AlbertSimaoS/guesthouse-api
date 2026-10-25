package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ConsumptionRepository
        extends JpaRepository<Consumption, Long> {

    Page<Consumption> findByReservationId(Long reservationId, Pageable pageable);

    @Query("""
    SELECT SUM(c.amount)
    FROM Consumption c
    WHERE c.reservation.id = :reservationId
""")
    BigDecimal sumConsumptionsByReservationId(
            @Param("reservationId") Long reservationId
    );
}