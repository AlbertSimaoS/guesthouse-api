
package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.SeasonRequestDTO;
import br.ifsp.guesthouse.dto.SeasonResponseDTO;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.repository.SeasonRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;

    public SeasonService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public SeasonResponseDTO register(SeasonRequestDTO request) {

        String name = request.name();

        if (seasonRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "Essa temporada já existe"
            );
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        BigDecimal multiplier = request.multiplier();

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "A data de término deve ser posterior ou igual à data de início"
            );
        }

        if (seasonRepository.existsOverlappingSeason(
                startDate,
                endDate
        )) {
            throw new IllegalArgumentException(
                    "O período informado se sobrepõe a uma temporada existente"
            );
        }

        if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "O multiplicador precisa ser maior que zero"
            );
        }

        Season season = new Season();
        season.setName(name);
        season.setStartDate(startDate);
        season.setEndDate(endDate);
        season.setMultiplier(multiplier);

        Season savedSeason = seasonRepository.save(season);

        return SeasonResponseDTO.fromEntity(savedSeason);
    }
}
