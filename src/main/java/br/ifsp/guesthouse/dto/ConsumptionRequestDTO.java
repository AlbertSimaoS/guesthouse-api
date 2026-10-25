package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.ConsumptionType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ConsumptionRequestDTO(

        @NotNull
        ConsumptionType type,

        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount

) {}