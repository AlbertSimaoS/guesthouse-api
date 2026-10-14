package br.ifsp.guesthouse.repository;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.UserAccount;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByNumberIgnoreCase(String number);
}
