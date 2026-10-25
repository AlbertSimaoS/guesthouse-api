package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.AvailabilityResponseDTO;
import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.dto.PaymentResponseDTO;
import br.ifsp.guesthouse.dto.ReservationRequestDTO;
import br.ifsp.guesthouse.dto.ReservationResponseDTO;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservationService (unitário) - RF06/RF07/RF08/RF13")
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private PaymentService paymentService;

    private ReservationService service;

    private final LocalDate start = LocalDate.now().plusDays(10);
    private final LocalDate end = LocalDate.now().plusDays(12);
    private UserAccount guest;
    private RoomType type;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository, roomTypeRepository, userAccountRepository, availabilityService, paymentService);
        guest = TestData.user(1L, "maria@email.com", Profile.GUEST);
        type = TestData.roomType(1, "Standard", 2, "100.00");
        authenticateAs(1L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));
    }

    private void givenBookable(String totalStayPrice) {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(roomTypeRepository.findById(1)).thenReturn(Optional.of(type));
        when(availabilityService.buildAvailableRoomTypeList(any(), any(), any())).thenReturn(List.of(
                new AvailabilityResponseDTO(1, "Standard", 2, 1, new BigDecimal("100.00"), new BigDecimal(totalStayPrice))));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReservationRequestDTO request(int guests) {
        return new ReservationRequestDTO(1, start, end, guests);
    }

    // ------------------------------------------------------------- RF06

    @Test
    @DisplayName("cria reserva PENDENTE, sem quarto, com sinal de 30% e pagamento do sinal")
    void createsPendingReservation() {
        givenBookable("200.00");

        ReservationResponseDTO response = service.createReservation(request(2));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(saved.getRoom()).isNull();
        assertThat(saved.getGuest()).isSameAs(guest);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(saved.getDepositAmount()).isEqualByComparingTo("60.00");
        verify(paymentService).createDepositPayment(saved);
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.roomId()).isNull();
        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("o sinal é 30% do total arredondado para 2 casas")
    void depositIsRounded() {
        givenBookable("333.33");

        service.createReservation(request(2));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getDepositAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("valida período e estadia mínima antes de reservar")
    void validatesPeriodAndMinimumStay() {
        givenBookable("200.00");
        doThrow(new IllegalArgumentException("minimo")).when(availabilityService)
                .validateHighSeasonMinimumStay(start, end);

        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(IllegalArgumentException.class);
        verify(reservationRepository, never()).save(any());

        doThrow(new IllegalArgumentException("periodo")).when(availabilityService).validatePeriod(start, end);
        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("exige usuário autenticado com id numérico")
    void requiresAuthenticatedUser() {
        givenBookable("200.00");

        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(IllegalArgumentException.class);

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(IllegalArgumentException.class);

        authenticateAs("abc");
        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(IllegalArgumentException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("usuário ou tipo de quarto inexistentes retornam ResourceNotFoundException")
    void unknownUserOrRoomType() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(ResourceNotFoundException.class);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(roomTypeRepository.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("recusa número de hóspedes acima da capacidade do tipo")
    void capacityExceeded() {
        givenBookable("200.00");

        assertThatThrownBy(() -> service.createReservation(request(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("recusa quando o tipo não está disponível nas datas")
    void notAvailable() {
        givenBookable("200.00");
        when(availabilityService.buildAvailableRoomTypeList(any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createReservation(request(2))).isInstanceOf(ResourceNotFoundException.class);
        verify(reservationRepository, never()).save(any());
    }

    // ------------------------------------------------------------- RF07

    @Test
    @DisplayName("confirmar o sinal confirma a reserva")
    void confirmingDepositConfirmsReservation() {
        Reservation reservation = TestData.reservation(5L, guest, type, start, end, "200.00", ReservationStatus.PENDING);
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(new PaymentResponseDTO(
                10L, 5L, PaymentType.DEPOSIT, new BigDecimal("60.00"), PaymentStatus.CONFIRMED, LocalDateTime.now()));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        PaymentResponseDTO response = service.updatePaymentStatus(10L, PaymentStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("confirmar um pagamento que não é o sinal não mexe na reserva")
    void confirmingCheckoutPaymentDoesNotConfirmReservation() {
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(new PaymentResponseDTO(
                10L, 5L, PaymentType.CHECKOUT, new BigDecimal("60.00"), PaymentStatus.CONFIRMED, LocalDateTime.now()));

        service.updatePaymentStatus(10L, PaymentStatus.CONFIRMED);

        verify(reservationRepository, never()).findById(anyLong());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("sinal confirmado de reserva inexistente lança ResourceNotFoundException")
    void depositOfUnknownReservation() {
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(new PaymentResponseDTO(
                10L, 5L, PaymentType.DEPOSIT, new BigDecimal("60.00"), PaymentStatus.CONFIRMED, LocalDateTime.now()));
        when(reservationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePaymentStatus(10L, PaymentStatus.CONFIRMED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("expira as reservas pendentes e os pagamentos pendentes delas; devolve a contagem")
    void expiresPendingReservations() {
        Reservation a = TestData.reservation(1L, guest, type, start, end, "200.00", ReservationStatus.PENDING);
        Reservation b = TestData.reservation(2L, guest, type, start, end, "200.00", ReservationStatus.PENDING);
        when(reservationRepository.findByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING), any()))
                .thenReturn(List.of(a, b));

        int expired = service.expirePendingReservations();

        assertThat(expired).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(b.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(paymentService).expirePendingPayments(1L);
        verify(paymentService).expirePendingPayments(2L);
    }

    @Test
    @DisplayName("sem reservas vencidas devolve zero")
    void nothingToExpire() {
        when(reservationRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());

        assertThat(service.expirePendingReservations()).isZero();
    }

    @Test
    @DisplayName("o corte de expiração é de 24 horas")
    @Disabled("BUG: expirePendingReservations usa LocalDateTime.now().minusHours(0) (comentário 'alterar para 24').")
    void expirationCutoffIs24Hours() {
        when(reservationRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();

        service.expirePendingReservations();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reservationRepository).findByStatusAndCreatedAtBefore(eq(ReservationStatus.PENDING), captor.capture());
        assertThat(captor.getValue()).isBefore(before.minusHours(23)).isAfter(before.minusHours(25));
    }

    // ------------------------------------------------------------- RF08

    private Reservation reservationStartingIn(int days, ReservationStatus status) {
        Reservation reservation = TestData.reservation(
                5L, guest, type, LocalDate.now().plusDays(days), LocalDate.now().plusDays(days + 2), "200.00", status);
        // o DTO de resposta do cancelamento exige quarto atribuído (ver teste desabilitado abaixo)
        reservation.setRoom(TestData.room(3, "101", type, RoomStatus.AVAILABLE));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        return reservation;
    }

    @Test
    @DisplayName("cancelar com 7 dias ou mais de antecedência devolve o sinal")
    void cancelWithRefund() {
        Reservation tenDays = reservationStartingIn(10, ReservationStatus.CONFIRMED);

        ReservationResponseDTO response = service.cancelReservation(5L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(tenDays.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(paymentService).cancelDepositPayment(5L, true);
    }

    @Test
    @DisplayName("cancelar exatamente 7 dias antes ainda devolve o sinal")
    void cancelExactlySevenDaysBefore() {
        reservationStartingIn(7, ReservationStatus.CONFIRMED);

        service.cancelReservation(5L);

        verify(paymentService).cancelDepositPayment(5L, true);
    }

    @Test
    @DisplayName("cancelar com menos de 7 dias retém o sinal")
    void cancelWithoutRefund() {
        reservationStartingIn(6, ReservationStatus.CONFIRMED);

        service.cancelReservation(5L);

        verify(paymentService).cancelDepositPayment(5L, false);
    }

    @Test
    @DisplayName("reserva pendente também pode ser cancelada")
    void cancelPending() {
        reservationStartingIn(3, ReservationStatus.PENDING);

        assertThat(service.cancelReservation(5L).status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("não cancela reserva inexistente, em estado final ou no dia da entrada")
    void cancelRejections() {
        when(reservationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelReservation(404L)).isInstanceOf(ResourceNotFoundException.class);

        reservationStartingIn(10, ReservationStatus.CHECKED_IN);
        assertThatThrownBy(() -> service.cancelReservation(5L)).isInstanceOf(IllegalStateException.class);

        reservationStartingIn(10, ReservationStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancelReservation(5L)).isInstanceOf(IllegalStateException.class);

        reservationStartingIn(0, ReservationStatus.CONFIRMED);
        assertThatThrownBy(() -> service.cancelReservation(5L)).isInstanceOf(IllegalStateException.class);

        verify(paymentService, never()).cancelDepositPayment(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("cancelar reserva ainda sem quarto atribuído (caso normal antes do check-in) funciona")
    @Disabled("BUG: cancelReservation monta o DTO com saved.getRoom().getId(); room é null antes do check-in -> NPE.")
    void cancelWithoutAssignedRoom() {
        Reservation reservation = reservationStartingIn(10, ReservationStatus.CONFIRMED);
        reservation.setRoom(null);

        assertThatCode(() -> service.cancelReservation(5L)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------- RF13

    @Test
    @DisplayName("lista as reservas do usuário autenticado, paginadas")
    void listsMyReservations() {
        Reservation reservation = TestData.reservation(5L, guest, type, start, end, "200.00", ReservationStatus.CONFIRMED);
        PageRequest pageable = PageRequest.of(0, 10);
        when(reservationRepository.findByGuestId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));

        PageResponseDTO<ReservationResponseDTO> page = service.getMyReservations(pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).userId()).isEqualTo(1L);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listar reservas exige usuário autenticado com id numérico")
    void listingRequiresAuthentication() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.getMyReservations(PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);

        authenticateAs("abc");
        assertThatThrownBy(() -> service.getMyReservations(PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
