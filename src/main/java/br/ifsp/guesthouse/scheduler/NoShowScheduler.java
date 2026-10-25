package br.ifsp.guesthouse.scheduler;

import br.ifsp.guesthouse.service.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoShowScheduler {

    private final ReservationService reservationService;

    public NoShowScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    public void checkNoShows() {

        int count = reservationService.processNoShows();

        System.out.println(
                "No-show processing completed. Reservations: " + count
        );
    }
}