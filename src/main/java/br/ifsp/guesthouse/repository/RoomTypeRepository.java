package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {
    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT rt
        FROM RoomType rt
        WHERE rt.capacity >= :numberOfGuests
          AND (
              SELECT COUNT(r) 
              FROM Room r 
              WHERE r.type = rt
          ) > (
              SELECT COUNT(res) 
              FROM Reservation res 
              WHERE res.roomType = rt
                AND res.status IN :statuses
                AND res.startDate <= :endDate
                AND res.endDate => :startDate
          )
    """)
    List<RoomType> findAvailableRoomTypes(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("numberOfGuests") Integer numberOfGuests,
            @Param("statuses") List<ReservationStatus> statuses
    );

}
