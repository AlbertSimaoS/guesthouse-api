package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

}