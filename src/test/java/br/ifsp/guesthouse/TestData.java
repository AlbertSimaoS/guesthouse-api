package br.ifsp.guesthouse;

import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fábricas de objetos de domínio para os testes unitários (sem banco de dados). */
public final class TestData {

    private TestData() {
    }

    public static RoomType roomType(int id, String name, int capacity, String dailyRate) {
        RoomType type = new RoomType();
        ReflectionTestUtils.setField(type, "id", id);
        type.setName(name);
        type.setCapacity(capacity);
        type.setDailyRate(new BigDecimal(dailyRate));
        return type;
    }

    public static Room room(int id, String number, RoomType type, RoomStatus status) {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", id);
        room.setNumber(number);
        room.setType(type);
        room.setStatus(status);
        return room;
    }

    public static UserAccount user(long id, String email, Profile profile) {
        UserAccount user = new UserAccount();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Usuario " + id);
        user.setCpf(String.format("%011d", id));
        user.setEmail(email);
        user.setTelephone("+5511999990000");
        user.setPassword("hash");
        user.setProfile(profile);
        return user;
    }

    public static Season season(String name, LocalDate start, LocalDate end, String multiplier) {
        Season season = new Season();
        season.setName(name);
        season.setStartDate(start);
        season.setEndDate(end);
        season.setMultiplier(new BigDecimal(multiplier));
        return season;
    }

    public static Reservation reservation(
            long id,
            UserAccount guest,
            RoomType type,
            LocalDate start,
            LocalDate end,
            String total,
            ReservationStatus status
    ) {
        Reservation reservation = new Reservation();
        ReflectionTestUtils.setField(reservation, "id", id);
        reservation.setGuest(guest);
        reservation.setRoomType(type);
        reservation.setStartDate(start);
        reservation.setEndDate(end);
        reservation.setNumberOfGuests(2);
        reservation.setTotalPrice(new BigDecimal(total));
        reservation.setDepositAmount(new BigDecimal(total).multiply(new BigDecimal("0.30")));
        reservation.setStatus(status);
        return reservation;
    }
}
