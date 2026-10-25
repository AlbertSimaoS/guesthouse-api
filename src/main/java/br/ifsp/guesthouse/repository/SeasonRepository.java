package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Integer> {
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

    List<Season> findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDate(
            LocalDate lastNight,
            LocalDate firstNight
    );

    Optional<Season> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate dateForStart,
            LocalDate dateForEnd
    );
}