package br.ifsp.guesthouse.model;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.AvailabilityResponseDTO;
import br.ifsp.guesthouse.dto.LoginResponseDTO;
import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.dto.RegisterResponseDTO;
import br.ifsp.guesthouse.dto.ReservationMovementDTO;
import br.ifsp.guesthouse.dto.ReservationResponseDTO;
import br.ifsp.guesthouse.dto.RoomResponseDTO;
import br.ifsp.guesthouse.dto.RoomTypeResponseDTO;
import br.ifsp.guesthouse.dto.SeasonResponseDTO;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entidades, enums e mapeamento para DTOs (unitário)")
class ModelAndDtoMappingTest {

    private final UserAccount guest = TestData.user(1L, "maria@email.com", Profile.GUEST);
    private final RoomType type = TestData.roomType(4, "Standard", 2, "100.00");

    @Test
    @DisplayName("RF01: as respostas de cadastro e login nunca carregam senha")
    void responsesNeverExposePassword() {
        assertThat(Arrays.stream(RegisterResponseDTO.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("password");
        assertThat(Arrays.stream(LoginResponseDTO.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("password");

        RegisterResponseDTO dto = RegisterResponseDTO.fromEntity(guest);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.email()).isEqualTo("maria@email.com");
        assertThat(dto.profile()).isEqualTo(Profile.GUEST);
        assertThat(LoginResponseDTO.fromEntity("tok", guest).accessToken()).isEqualTo("tok");
    }

    @Test
    @DisplayName("padrões das entidades: usuário GUEST, quarto livre, reserva e pagamento pendentes")
    void entityDefaults() {
        assertThat(new UserAccount().getProfile()).isEqualTo(Profile.GUEST);
        assertThat(new Room().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(new Reservation().getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(new Reservation().getCreatedAt()).isNotNull();
        Payment payment = new Payment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(new RoomType().getRooms()).isEmpty();
    }

    @Test
    @DisplayName("RF03: o quarto tem os 4 estados previstos e registra o instante da mudança")
    void roomStatuses() {
        assertThat(RoomStatus.values()).containsExactlyInAnyOrder(
                RoomStatus.AVAILABLE, RoomStatus.OCCUPIED, RoomStatus.CLEANING, RoomStatus.MAINTENANCE);
        Room room = new Room();
        assertThat(room.getStatusUpdatedAt()).isNull();
        room.setStatus(RoomStatus.CLEANING);
        assertThat(room.getStatusUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("perfis e status de reserva cobrem o ciclo do enunciado")
    void enums() {
        assertThat(Profile.values()).containsExactlyInAnyOrder(Profile.GUEST, Profile.RECEPTION);
        assertThat(ReservationStatus.values()).contains(
                ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.EXPIRED,
                ReservationStatus.NO_SHOW, ReservationStatus.CANCELLED, ReservationStatus.CHECKED_IN,
                ReservationStatus.FINISHED);
    }

    @Test
    @DisplayName("ReservationResponseDTO: roomId é nulo sem quarto atribuído e preenchido depois do check-in")
    void reservationResponseMapping() {
        Reservation reservation = TestData.reservation(
                9L, guest, type, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), "200.00", ReservationStatus.PENDING);

        ReservationResponseDTO withoutRoom = ReservationResponseDTO.from(reservation);
        assertThat(withoutRoom.roomId()).isNull();
        assertThat(withoutRoom.id()).isEqualTo(9L);
        assertThat(withoutRoom.roomTypeId()).isEqualTo(4);
        assertThat(withoutRoom.userId()).isEqualTo(1L);
        assertThat(withoutRoom.status()).isEqualTo(ReservationStatus.PENDING);

        reservation.setRoom(TestData.room(7, "101", type, RoomStatus.OCCUPIED));
        assertThat(ReservationResponseDTO.from(reservation).roomId()).isEqualTo(7);
    }

    @Test
    @DisplayName("mapeia tipo, quarto, temporada e disponibilidade para os DTOs")
    void simpleMappings() {
        RoomTypeResponseDTO typeDto = RoomTypeResponseDTO.fromEntity(type);
        assertThat(typeDto.id()).isEqualTo(4);
        assertThat(typeDto.dailyRate()).isEqualByComparingTo("100.00");

        RoomResponseDTO roomDto = new RoomResponseDTO(TestData.room(7, "101", type, RoomStatus.CLEANING));
        assertThat(roomDto.number()).isEqualTo("101");
        assertThat(roomDto.status()).isEqualTo(RoomStatus.CLEANING);
        assertThat(roomDto.typeName()).isEqualTo("Standard");

        Season season = TestData.season("Alta", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), "1.5");
        SeasonResponseDTO seasonDto = SeasonResponseDTO.fromEntity(season);
        assertThat(seasonDto.name()).isEqualTo("Alta");
        assertThat(seasonDto.multiplier()).isEqualByComparingTo("1.5");

        AvailabilityResponseDTO availability = AvailabilityResponseDTO.from(type, 3, new BigDecimal("300.00"));
        assertThat(availability.roomTypeId()).isEqualTo(4);
        assertThat(availability.availableRooms()).isEqualTo(3);
        assertThat(availability.nightlyRate()).isEqualByComparingTo("100.00");
        assertThat(availability.totalStayPrice()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("ReservationMovementDTO usa 'Desconhecido' quando não há hóspede")
    void movementDto() {
        Reservation withGuest = TestData.reservation(
                1L, guest, type, LocalDate.now(), LocalDate.now().plusDays(1), "100", ReservationStatus.CONFIRMED);
        assertThat(new ReservationMovementDTO(withGuest).guestName()).isEqualTo(guest.getName());
        assertThat(new ReservationMovementDTO(withGuest).roomTypeName()).isEqualTo("Standard");

        Reservation withoutGuest = new Reservation();
        withoutGuest.setRoomType(type);
        assertThat(new ReservationMovementDTO(withoutGuest).guestName()).isEqualTo("Desconhecido");
    }

    @Test
    @DisplayName("PageResponseDTO copia os metadados da página")
    void pageMapping() {
        PageResponseDTO<String> dto = PageResponseDTO.from(new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5));

        assertThat(dto.content()).containsExactly("a", "b");
        assertThat(dto.page()).isZero();
        assertThat(dto.size()).isEqualTo(2);
        assertThat(dto.totalElements()).isEqualTo(5);
        assertThat(dto.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("PasswordResetToken: válido, expirado, usado e validações do construtor")
    void passwordResetToken() {
        PasswordResetToken valid = new PasswordResetToken("h", guest, LocalDateTime.now().plusMinutes(10));
        assertThat(valid.isValid()).isTrue();
        assertThat(valid.isExpired()).isFalse();
        valid.markAsUsed();
        assertThat(valid.isUsed()).isTrue();
        assertThat(valid.isValid()).isFalse();

        PasswordResetToken expired = new PasswordResetToken("h", guest, LocalDateTime.now().minusSeconds(1));
        assertThat(expired.isExpired()).isTrue();
        assertThat(expired.isValid()).isFalse();

        assertThatThrownBy(() -> new PasswordResetToken(null, guest, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PasswordResetToken("h", null, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PasswordResetToken("h", guest, null))
                .isInstanceOf(NullPointerException.class);

        valid.setTokenHash("novo");
        valid.setUser(guest);
        valid.setExpiresAt(LocalDateTime.now().plusDays(1));
        valid.setUsed(false);
        assertThat(valid.getTokenHash()).isEqualTo("novo");
        assertThat(valid.getUser()).isSameAs(guest);
        assertThat(valid.isValid()).isTrue();
    }

    @Test
    @DisplayName("getters e setters das entidades")
    void gettersAndSetters() {
        type.setName("Luxo");
        type.setCapacity(4);
        type.setDailyRate(new BigDecimal("500"));
        type.setRooms(List.of());
        assertThat(type.getName()).isEqualTo("Luxo");
        assertThat(type.getCapacity()).isEqualTo(4);
        assertThat(type.getRooms()).isEmpty();

        guest.setName("Outro");
        guest.setCpf("99999999999");
        guest.setTelephone("+5511000000000");
        guest.setPassword("x");
        assertThat(guest.getName()).isEqualTo("Outro");
        assertThat(guest.getCpf()).isEqualTo("99999999999");
        assertThat(guest.getTelephone()).isEqualTo("+5511000000000");
        assertThat(guest.getPassword()).isEqualTo("x");

        Payment payment = new Payment();
        Reservation reservation = new Reservation();
        payment.setReservation(reservation);
        payment.setAmount(BigDecimal.TEN);
        assertThat(payment.getReservation()).isSameAs(reservation);
        assertThat(payment.getAmount()).isEqualByComparingTo("10");

        Room room = new Room();
        room.setStatusUpdatedAt(LocalDateTime.now());
        room.setType(type);
        assertThat(room.getType()).isSameAs(type);
        assertThat(room.getStatusUpdatedAt()).isNotNull();
    }
}
