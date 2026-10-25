package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStatusAndCreatedAtBefore(
            ReservationStatus status,
            LocalDateTime dateTime
    );

    @Query("""
            SELECT COUNT(res) FROM Reservation res
            WHERE res.roomType.id = :roomTypeId
              AND res.status IN :statuses
              AND res.startDate < :endDate
              AND res.endDate > :startDate
            """)
    long countBlockingReservations(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ReservationStatus> statuses
    );

    @Query("""
    SELECT COUNT(r) FROM Reservation r
    WHERE r.room.id = :roomId
      AND r.status = :status
      AND r.startDate < :endDate
      AND r.endDate > :startDate
""")
    long countRoomReservations(
            @Param("roomId") Integer roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ReservationStatus status
    );

    List<Reservation> findByStatusAndStartDateBefore(
            ReservationStatus status,
            LocalDate date
    );

    Page<Reservation> findByGuestId(Long guestId, Pageable pageable);
}