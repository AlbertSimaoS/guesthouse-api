package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckoutResponseDTO(
        Long reservationId,
        Long paymentId,
        LocalDate checkoutDate,
        boolean earlyCheckout,
        BigDecimal stayPrice,
        BigDecimal consumptionTotal,
        BigDecimal penalty,
        BigDecimal depositAmount,
        BigDecimal totalDue,
        BigDecimal checkoutAmount,
        PaymentStatus paymentStatus
) {}