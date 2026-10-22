package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.Season;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonResponseDTO (
        Integer id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal multiplier
) {
    public static SeasonResponseDTO fromEntity(Season season){
        return new SeasonResponseDTO(
                season.getId(),
                season.getName(),
                season.getStartDate(),
                season.getEndDate(),
                season.getMultiplier()
        );
    }
}
