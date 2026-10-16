package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.dto.SeasonRequestDTO;
import br.ifsp.guesthouse.dto.SeasonResponseDTO;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String name = request.name().trim();

        if (seasonRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A season with this name already exists."
            );
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        BigDecimal multiplier = request.multiplier();

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "The end date must be on or after the start date."
            );
        }

        if (seasonRepository.existsOverlappingSeason(startDate, endDate)) {
            throw new IllegalArgumentException(
                    "The specified period overlaps with an existing season."
            );
        }

        if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "The multiplier must be greater than zero."
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