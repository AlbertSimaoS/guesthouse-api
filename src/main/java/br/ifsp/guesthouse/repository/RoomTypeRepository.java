package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {
    boolean existsByNameIgnoreCase(String name);

    List<RoomType> findByCapacityGreaterThanEqual(Integer capacity);
}
