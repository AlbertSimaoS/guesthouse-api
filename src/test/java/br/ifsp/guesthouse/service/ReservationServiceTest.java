package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.AvailabilityResponseDTO;
import br.ifsp.guesthouse.dto.CheckoutResponseDTO;
import br.ifsp.guesthouse.dto.PageResponseDTO;
import br.ifsp.guesthouse.dto.PaymentResponseDTO;
import br.ifsp.guesthouse.dto.ReservationRequestDTO;
import br.ifsp.guesthouse.dto.ReservationResponseDTO;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.Room;
import br.ifsp.guesthouse.model.RoomType;
import br.ifsp.guesthouse.model.UserAccount;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.model.enums.RoomStatus;
import br.ifsp.guesthouse.repository.ConsumptionRepository;
import br.ifsp.guesthouse.repository.ReservationRepository;
import br.ifsp.guesthouse.repository.RoomRepository;
import br.ifsp.guesthouse.repository.RoomTypeRepository;
import br.ifsp.guesthouse.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock private RoomRepository roomRepository;
    @Mock private ConsumptionRepository consumptionRepository;

    private ReservationService service;

    private final LocalDate start = LocalDate.now().plusDays(10);
    private final LocalDate end = LocalDate.now().plusDays(12);
    private UserAccount guest;
    private RoomType type;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, roomTypeRepository,userAccountRepository,availabilityService,paymentService, roomRepository, consumptionRepository);
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

        PaymentResponseDTO response = service.updateReservationStatus(10L, PaymentStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("sinal confirmado de reserva inexistente lança ResourceNotFoundException")
    void depositOfUnknownReservation() {
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(new PaymentResponseDTO(
                10L, 5L, PaymentType.DEPOSIT, new BigDecimal("60.00"), PaymentStatus.CONFIRMED, LocalDateTime.now()));
        when(reservationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReservationStatus(10L, PaymentStatus.CONFIRMED))
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
    void cancelWithoutAssignedRoom() {
        Reservation reservation = reservationStartingIn(10, ReservationStatus.CONFIRMED);

        assertThat(reservation.getRoom()).isNull();
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

    // ------------------------------------------- RF07/RF11: updateReservationStatus

    private PaymentResponseDTO confirmed(PaymentType paymentType) {
        return new PaymentResponseDTO(
                10L, 5L, paymentType, new BigDecimal("60.00"), PaymentStatus.CONFIRMED, LocalDateTime.now());
    }

    private Reservation reservationWith(ReservationStatus status, Room room) {
        Reservation reservation = TestData.reservation(5L, guest, type, start, end, "200.00", status);
        reservation.setRoom(room);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        return reservation;
    }

    @Test
    @DisplayName("sinal só confirma reserva PENDENTE")
    void depositOnlyConfirmsPendingReservation() {
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(confirmed(PaymentType.DEPOSIT));
        reservationWith(ReservationStatus.CANCELLED, null);

        assertThatThrownBy(() -> service.updateReservationStatus(10L, PaymentStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("pagamento do check-out finaliza a reserva e manda o quarto para limpeza")
    void checkoutPaymentFinishesReservation() {
        Room room = TestData.room(3, "101", type, RoomStatus.OCCUPIED);
        Reservation reservation = reservationWith(ReservationStatus.CHECKED_IN, room);
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(confirmed(PaymentType.CHECKOUT));

        service.updateReservationStatus(10L, PaymentStatus.CONFIRMED);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FINISHED);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLEANING);
        verify(roomRepository).save(room);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("pagamento do check-out exige reserva em CHECKED_IN, com quarto ocupado")
    void checkoutPaymentRejections() {
        when(paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED)).thenReturn(confirmed(PaymentType.CHECKOUT));

        reservationWith(ReservationStatus.CONFIRMED, null);
        assertThatThrownBy(() -> service.updateReservationStatus(10L, PaymentStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);

        reservationWith(ReservationStatus.CHECKED_IN, null);
        assertThatThrownBy(() -> service.updateReservationStatus(10L, PaymentStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);

        reservationWith(ReservationStatus.CHECKED_IN, TestData.room(3, "101", type, RoomStatus.CLEANING));
        assertThatThrownBy(() -> service.updateReservationStatus(10L, PaymentStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);

        verify(roomRepository, never()).save(any());
    }

    // ------------------------------------------------------------- RF09: check-in

    private Reservation confirmedStartingToday() {
        Reservation reservation = TestData.reservation(
                5L, guest, type, LocalDate.now(), LocalDate.now().plusDays(2), "200.00", ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        return reservation;
    }

    @Test
    @DisplayName("check-in atribui o quarto livre do tipo reservado, ocupa o quarto e marca CHECKED_IN")
    void checkInAssignsRoom() {
        Reservation reservation = confirmedStartingToday();
        Room room = TestData.room(3, "101", type, RoomStatus.AVAILABLE);
        when(roomRepository.findById(3)).thenReturn(Optional.of(room));

        ReservationResponseDTO response = service.checkIn(5L, 3);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(reservation.getRoom()).isSameAs(room);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        assertThat(response.status()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(response.roomId()).isEqualTo(3);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("check-in: reserva inexistente, não confirmada ou fora da data de entrada é recusada")
    void checkInReservationRejections() {
        when(reservationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkIn(404L, 3)).isInstanceOf(ResourceNotFoundException.class);

        Reservation pending = confirmedStartingToday();
        pending.setStatus(ReservationStatus.PENDING);
        assertThatThrownBy(() -> service.checkIn(5L, 3)).isInstanceOf(IllegalStateException.class);

        Reservation tomorrow = confirmedStartingToday();
        tomorrow.setStartDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.checkIn(5L, 3)).isInstanceOf(IllegalStateException.class);

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("check-in: quarto inexistente, de outro tipo ou indisponível é recusado")
    void checkInRoomRejections() {
        confirmedStartingToday();

        when(roomRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkIn(5L, 99)).isInstanceOf(ResourceNotFoundException.class);

        RoomType other = TestData.roomType(2, "Suite", 3, "300.00");
        when(roomRepository.findById(4)).thenReturn(Optional.of(TestData.room(4, "201", other, RoomStatus.AVAILABLE)));
        assertThatThrownBy(() -> service.checkIn(5L, 4)).isInstanceOf(IllegalArgumentException.class);

        when(roomRepository.findById(5)).thenReturn(Optional.of(TestData.room(5, "102", type, RoomStatus.CLEANING)));
        assertThatThrownBy(() -> service.checkIn(5L, 5)).isInstanceOf(IllegalStateException.class);

        verify(roomRepository, never()).save(any());
    }

    // ------------------------------------------------------------- RF09: no-show

    @Test
    @DisplayName("não comparecimento: confirmada sem check-in vira NO_SHOW e o sinal fica retido")
    void processNoShows() {
        Reservation a = TestData.reservation(1L, guest, type, LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1), "200.00", ReservationStatus.CONFIRMED);
        Reservation b = TestData.reservation(2L, guest, type, LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(1), "200.00", ReservationStatus.CONFIRMED);
        when(reservationRepository.findByStatusAndStartDateBefore(ReservationStatus.CONFIRMED, LocalDate.now()))
                .thenReturn(List.of(a, b));

        int processed = service.processNoShows();

        assertThat(processed).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(b.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        verify(paymentService).retainDepositPayment(1L);
        verify(paymentService).retainDepositPayment(2L);
    }

    @Test
    @DisplayName("sem não comparecimentos devolve zero")
    void noNoShows() {
        when(reservationRepository.findByStatusAndStartDateBefore(any(), any())).thenReturn(List.of());

        assertThat(service.processNoShows()).isZero();
        verify(paymentService, never()).retainDepositPayment(anyLong());
    }

    // ------------------------------------------------------------ RF10/RF11: check-out

    private Reservation checkedIn(LocalDate from, LocalDate to) {
        Reservation reservation = TestData.reservation(5L, guest, type, from, to, "200.00", ReservationStatus.CHECKED_IN);
        reservation.setRoom(TestData.room(3, "101", type, RoomStatus.OCCUPIED));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        return reservation;
    }

    private void givenCheckoutData(String consumptions, String deposit) {
        when(consumptionRepository.sumConsumptionsByReservationId(5L)).thenReturn(new BigDecimal(consumptions));
        Payment depositPayment = new Payment();
        depositPayment.setAmount(new BigDecimal(deposit));
        when(paymentService.getConfirmedDeposit(5L)).thenReturn(depositPayment);
        when(paymentService.createCheckoutPayment(any(), any())).thenAnswer(inv -> {
            Payment payment = new Payment();
            payment.setAmount(inv.getArgument(1));
            return payment;
        });
    }

    @Test
    @DisplayName("check-out na data prevista: diárias + consumos - sinal, sem multa")
    void regularCheckout() {
        LocalDate today = LocalDate.now();
        Reservation reservation = checkedIn(today.minusDays(2), today);
        givenCheckoutData("50.00", "60.00");

        CheckoutResponseDTO dto = service.checkOut(5L);

        assertThat(dto.earlyCheckout()).isFalse();
        assertThat(dto.stayPrice()).isEqualByComparingTo("200.00");
        assertThat(dto.penalty()).isEqualByComparingTo("0");
        assertThat(dto.consumptionTotal()).isEqualByComparingTo("50.00");
        assertThat(dto.depositAmount()).isEqualByComparingTo("60.00");
        assertThat(dto.totalDue()).isEqualByComparingTo("250.00");
        assertThat(dto.checkoutAmount()).isEqualByComparingTo("190.00");
        assertThat(dto.checkoutDate()).isEqualTo(today);
        assertThat(dto.reservationId()).isEqualTo(5L);
        verify(paymentService).createCheckoutPayment(eq(reservation), any());
    }

    @Test
    @DisplayName("saída antecipada: cobra as diárias usadas + 1 diária de multa")
    void earlyCheckout() {
        LocalDate today = LocalDate.now();
        checkedIn(today.minusDays(2), today.plusDays(3));
        givenCheckoutData("50.00", "60.00");
        // 2 diárias usadas (início -> hoje) = 200; multa = 1 diária (amanhã) = 100
        when(availabilityService.calculateStayPrice(eq(type), eq(today.minusDays(2)), eq(today)))
                .thenReturn(new BigDecimal("200.00"));
        when(availabilityService.calculateStayPrice(eq(type), eq(today.plusDays(1)), eq(today.plusDays(2))))
                .thenReturn(new BigDecimal("100.00"));

        CheckoutResponseDTO dto = service.checkOut(5L);

        assertThat(dto.earlyCheckout()).isTrue();
        assertThat(dto.stayPrice()).isEqualByComparingTo("200.00");
        assertThat(dto.penalty()).isEqualByComparingTo("100.00");
        // 200 + 50 + 100 = 350; menos sinal 60 = 290
        assertThat(dto.totalDue()).isEqualByComparingTo("350.00");
        assertThat(dto.checkoutAmount()).isEqualByComparingTo("290.00");
    }

    @Test
    @DisplayName("saída antecipada no mesmo dia do check-in cobra pelo menos 1 diária")
    void earlyCheckoutSameDayChargesOneNight() {
        LocalDate today = LocalDate.now();
        checkedIn(today, today.plusDays(2));
        givenCheckoutData("0.00", "30.00");
        when(availabilityService.calculateStayPrice(eq(type), eq(today), eq(today.plusDays(1))))
                .thenReturn(new BigDecimal("100.00"));
        when(availabilityService.calculateStayPrice(eq(type), eq(today.plusDays(1)), eq(today.plusDays(2))))
                .thenReturn(new BigDecimal("100.00"));

        CheckoutResponseDTO dto = service.checkOut(5L);

        assertThat(dto.stayPrice()).isEqualByComparingTo("100.00");
        assertThat(dto.penalty()).isEqualByComparingTo("100.00");
        assertThat(dto.checkoutAmount()).isEqualByComparingTo("170.00");
    }

    @Test
    @DisplayName("check-out: reserva inexistente, fora de CHECKED_IN ou sem quarto é recusada")
    void checkoutRejections() {
        when(reservationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkOut(404L)).isInstanceOf(ResourceNotFoundException.class);

        Reservation confirmed = checkedIn(LocalDate.now().minusDays(1), LocalDate.now());
        confirmed.setStatus(ReservationStatus.CONFIRMED);
        assertThatThrownBy(() -> service.checkOut(5L)).isInstanceOf(IllegalStateException.class);

        Reservation noRoom = checkedIn(LocalDate.now().minusDays(1), LocalDate.now());
        noRoom.setRoom(null);
        assertThatThrownBy(() -> service.checkOut(5L)).isInstanceOf(IllegalStateException.class);

        verify(paymentService, never()).createCheckoutPayment(any(), any());
    }

    @Test
    @DisplayName("check-out com saldo zero ou negativo (sinal cobre tudo) é recusado pelo service")
    void checkoutWithNonPositiveBalance() {
        LocalDate today = LocalDate.now();
        checkedIn(today.minusDays(2), today);
        givenCheckoutData("0.00", "300.00");

        assertThatThrownBy(() -> service.checkOut(5L)).isInstanceOf(IllegalStateException.class);
        verify(paymentService, never()).createCheckoutPayment(any(), any());
    }

    // ------------------------------------------------- regras adicionais

    private void authenticateAs(Object principal, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("cancelar: o hóspede não cancela a reserva de outro hóspede")
    void guestCannotCancelSomeoneElsesReservation() {
        Reservation reservation = reservationStartingIn(10, ReservationStatus.CONFIRMED);
        authenticateAs(99L, "ROLE_GUEST");

        assertThatThrownBy(() -> service.cancelReservation(5L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(paymentService, never()).cancelDepositPayment(anyLong(), anyBoolean());
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("cancelar: a recepção cancela a reserva de qualquer hóspede")
    void receptionCanCancelAnyReservation() {
        reservationStartingIn(10, ReservationStatus.CONFIRMED);
        authenticateAs(99L, "ROLE_RECEPTION");

        assertThat(service.cancelReservation(5L).status()).isEqualTo(ReservationStatus.CANCELLED);
        verify(paymentService).cancelDepositPayment(5L, true);
    }

    @Test
    @DisplayName("check-out: estadia sem consumos (soma nula) não causa NullPointerException")
    void checkoutWithoutConsumptions() {
        LocalDate today = LocalDate.now();
        checkedIn(today.minusDays(2), today);
        givenCheckoutData("0.00", "60.00");
        when(consumptionRepository.sumConsumptionsByReservationId(5L)).thenReturn(null);

        CheckoutResponseDTO response = service.checkOut(5L);

        assertThat(response.consumptionTotal()).isEqualByComparingTo("0.00");
        assertThat(response.totalDue()).isEqualByComparingTo("200.00");
        assertThat(response.checkoutAmount()).isEqualByComparingTo("140.00");
    }
}
