package br.ifsp.guesthouse.dto;

import br.ifsp.guesthouse.model.Reservation;

public record ReservationMovementDTO(
        Long reservationId,
        String roomTypeName,
        Integer numberOfGuests,
        String guestName
) {
    public ReservationMovementDTO(Reservation reservation) {
        this(
                reservation.getId(),
                reservation.getRoomType().getName(),
                reservation.getNumberOfGuests(),
                reservation.getGuest() != null ? reservation.getGuest().getName() : "Desconhecido"
        );
    }
}