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

    Page<Reservation> findByGuestId(Long guestId, Pageable pageable);

    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.status IN :statuses 
          AND r.startDate <= :endDate 
          AND r.endDate >= :startDate
        """)
    List<Reservation> findActiveReservationsInPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ReservationStatus> statuses
    );

    @Query("""
    SELECT r FROM Reservation r
    WHERE r.roomType.id = :roomTypeId
      AND r.status IN :statuses
      AND r.startDate <= :endDate
      AND r.endDate >= :startDate
    """)
    List<Reservation> findByRoomTypeIdAndPeriod(
            @Param("roomTypeId") Integer roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ReservationStatus> statuses
    );

    List<Reservation> findByStartDate(LocalDate startDate);
    List<Reservation> findByEndDate(LocalDate endDate);
}