package br.ifsp.guesthouse.functional;

import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.Season;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.PaymentRepository;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.SeasonRepository;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes funcionais: sobe a aplicação inteira (Spring Security + JWT + JPA)
 * com H2 em memória e exercita a API via MockMvc, como um cliente HTTP faria.
 *
 * O banco é limpo antes de cada teste. Os testes NÃO são @Transactional de propósito:
 * assim cada requisição abre e fecha a sua própria transação, como em produção
 * (com open-in-view=false), e problemas de lazy loading aparecem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractFunctionalTest {

    protected static final String PASSWORD = "senha12345";

    private static final AtomicLong CPF_SEQ = new AtomicLong(10_000_000_000L);

    @Autowired protected MockMvc mockMvc;
    @Autowired protected DataSource dataSource;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected UserAccountRepository userRepository;
    @Autowired protected RoomTypeRepository roomTypeRepository;
    @Autowired protected RoomRepository roomRepository;
    @Autowired protected SeasonRepository seasonRepository;
    @Autowired protected ReservationRepository reservationRepository;
    @Autowired protected PaymentRepository paymentRepository;

    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DELETE FROM consumptions");
        jdbc.execute("DELETE FROM payments");
        jdbc.execute("DELETE FROM reservations");
        jdbc.execute("DELETE FROM password_reset_tokens");
        jdbc.execute("DELETE FROM rooms");
        jdbc.execute("DELETE FROM room_types");
        jdbc.execute("DELETE FROM seasons");
        jdbc.execute("DELETE FROM user_account");
    }

    // ------------------------------------------------------------------ datas

    protected static LocalDate day(int plusDays) {
        return LocalDate.now().plusDays(plusDays);
    }

    // ------------------------------------------------------------------ HTTP

    protected ResultActions doGet(String url, String token) throws Exception {
        var builder = MockMvcRequestBuilders.get(url);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    protected ResultActions doPost(String url, String token, String body) throws Exception {
        var builder = MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    protected ResultActions doPatch(String url, String token, String body) throws Exception {
        var builder = MockMvcRequestBuilders.patch(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            builder.content(body);
        }
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    protected static <T> T read(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    protected static BigDecimal number(MvcResult result, String path) throws Exception {
        Object value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return new BigDecimal(value.toString());
    }

    // ------------------------------------------------------------ autenticação

    protected String login(String email) throws Exception {
        MvcResult result = doPost(
                "/auth/login",
                null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)
        ).andExpect(status().isOk()).andReturn();
        return read(result, "$.accessToken");
    }

    protected UserAccount createUser(String email, Profile profile) {
        UserAccount user = new UserAccount();
        user.setName(profile == Profile.RECEPTION ? "Recepcao" : "Hospede " + email);
        user.setCpf(String.format("%011d", CPF_SEQ.incrementAndGet()));
        user.setEmail(email);
        user.setTelephone("+5511999990000");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setProfile(profile);
        return userRepository.save(user);
    }

    protected UserAccount createGuest(String email) {
        return createUser(email, Profile.GUEST);
    }

    protected UserAccount createReception() {
        return createUser("recepcao@pousada.com", Profile.RECEPTION);
    }

    protected String guestToken(String email) throws Exception {
        createGuest(email);
        return login(email);
    }

    protected String receptionToken() throws Exception {
        createReception();
        return login("recepcao@pousada.com");
    }

    // ---------------------------------------------------------------- fixtures

    protected RoomType createRoomType(String name, int capacity, String dailyRate) {
        RoomType type = new RoomType();
        type.setName(name);
        type.setCapacity(capacity);
        type.setDailyRate(new BigDecimal(dailyRate));
        return roomTypeRepository.save(type);
    }

    protected Room createRoom(String number, RoomType type, RoomStatus status) {
        Room room = new Room();
        room.setNumber(number);
        room.setType(type);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    protected Season createSeason(String name, LocalDate start, LocalDate end, String multiplier) {
        Season season = new Season();
        season.setName(name);
        season.setStartDate(start);
        season.setEndDate(end);
        season.setMultiplier(new BigDecimal(multiplier));
        return seasonRepository.save(season);
    }

    protected Reservation createReservation(
            UserAccount guest,
            RoomType type,
            LocalDate start,
            LocalDate end,
            int guests,
            String totalPrice,
            ReservationStatus status
    ) {
        BigDecimal total = new BigDecimal(totalPrice);
        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoomType(type);
        reservation.setStartDate(start);
        reservation.setEndDate(end);
        reservation.setNumberOfGuests(guests);
        reservation.setTotalPrice(total);
        reservation.setDepositAmount(total.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP));
        reservation.setStatus(status);
        return reservationRepository.save(reservation);
    }

    protected Payment createPayment(Reservation reservation, PaymentType type, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setType(type);
        payment.setAmount(reservation.getDepositAmount());
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    /** Simula a passagem do tempo: recua a data de criação da reserva. */
    protected void backdateReservation(Long reservationId, int hoursAgo) {
        jdbc.update(
                "UPDATE reservations SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(hoursAgo)),
                reservationId
        );
    }

    protected static String reservationJson(Integer roomTypeId, LocalDate start, LocalDate end, int guests) {
        return """
                {"roomTypeId":%d,"startDate":"%s","endDate":"%s","numberOfGuests":%d}
                """.formatted(roomTypeId, start, end, guests);
    }

    protected static String availabilityUrl(LocalDate start, LocalDate end, int guests) {
        return "/room-types/availability?startDate=%s&endDate=%s&numberOfGuests=%d"
                .formatted(start, end, guests);
    }
}
