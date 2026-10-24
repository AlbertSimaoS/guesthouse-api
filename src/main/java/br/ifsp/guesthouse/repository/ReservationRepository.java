package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {
    @Query("""
        SELECT COUNT(res) FROM Reservation res 
        WHERE res.roomType.id = :roomTypeId
          AND res.status IN :statuses
          AND res.startDate < :endDate
          AND res.endDate > :startDate
    """)
    int countBlockingReservations(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ReservationStatus> statuses
    );
}