package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Season;
import org.hibernate.boot.models.JpaAnnotations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM Season s
        WHERE s.startDate <= :endDate
          AND s.endDate >= :startDate
    """)
    boolean existsOverlappingSeason(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<Season> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate dateForStart,
            LocalDate dateForEnd
    );

    List<Season> findAllByStartDateLessThanAndEndDateGreaterThan(
            LocalDate checkOut,
            LocalDate checkIn
    );
}
