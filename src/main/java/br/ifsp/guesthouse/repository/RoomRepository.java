package br.ifsp.guesthouse.repository;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByNumberIgnoreCase(String number);

    @Query("""
    SELECT r
    FROM Room r
    WHERE r.type.capacity >= :guests
      AND NOT EXISTS (
          SELECT reservation.id
          FROM Reservation reservation
          WHERE reservation.room = r
            AND reservation.status IN :statuses
            AND reservation.checkIn < :checkOut
            AND reservation.checkOut > :checkIn
      )
    """)
    List<Room> findAvailableRoomsUnpaged(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("guests") Integer guests,
            @Param("statuses") List<ReservationStatus> statuses
    );
}
