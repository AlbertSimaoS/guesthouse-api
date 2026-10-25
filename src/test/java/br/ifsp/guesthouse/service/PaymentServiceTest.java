package br.ifsp.guesthouse.service;

import br.ifsp.guesthouse.TestData;
import br.ifsp.guesthouse.dto.PaymentResponseDTO;
import br.ifsp.guesthouse.exception.ResourceNotFoundException;
import br.ifsp.guesthouse.model.Payment;
import br.ifsp.guesthouse.model.Reservation;
import br.ifsp.guesthouse.model.enums.PaymentStatus;
import br.ifsp.guesthouse.model.enums.PaymentType;
import br.ifsp.guesthouse.model.enums.Profile;
import br.ifsp.guesthouse.model.enums.ReservationStatus;
import br.ifsp.guesthouse.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService (unitário) - RF07/RF08")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private PaymentService paymentService;

    private Reservation reservation() {
        Reservation reservation = TestData.reservation(
                5L,
                TestData.user(1L, "maria@email.com", Profile.GUEST),
                TestData.roomType(1, "Standard", 2, "100.00"),
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                "200.00", ReservationStatus.PENDING);
        return reservation;
    }

    private Payment payment(PaymentType type, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setReservation(reservation());
        payment.setType(type);
        payment.setAmount(new BigDecimal("60.00"));
        payment.setStatus(status);
        return payment;
    }

    @Test
    @DisplayName("createDepositPayment: cria o sinal PENDENTE com o valor do sinal da reserva")
    void createsDeposit() {
        Reservation reservation = reservation();

        paymentService.createDepositPayment(reservation);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getAmount()).isEqualByComparingTo(reservation.getDepositAmount());
        assertThat(saved.getReservation()).isSameAs(reservation);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updatePaymentStatus: confirma um pagamento pendente")
    void confirmsPendingPayment() {
        Payment pending = payment(PaymentType.DEPOSIT, PaymentStatus.PENDING);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponseDTO response = paymentService.updatePaymentStatus(10L, PaymentStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(response.type()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(response.reservationId()).isEqualTo(5L);
        assertThat(response.amount()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("updatePaymentStatus: inexistente, não pendente ou status diferente de CONFIRMED são rejeitados")
    void rejectsInvalidUpdates() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(PaymentType.DEPOSIT, PaymentStatus.CONFIRMED)));
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(payment(PaymentType.DEPOSIT, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(404L, PaymentStatus.CONFIRMED))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, PaymentStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(2L, PaymentStatus.REFUNDED))
                .isInstanceOf(IllegalArgumentException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("expirePendingPayments: marca os pagamentos pendentes da reserva como EXPIRED")
    void expiresPendingPayments() {
        Payment a = payment(PaymentType.DEPOSIT, PaymentStatus.PENDING);
        Payment b = payment(PaymentType.CHECKOUT, PaymentStatus.PENDING);
        when(paymentRepository.findByReservationIdAndStatus(5L, PaymentStatus.PENDING)).thenReturn(List.of(a, b));

        paymentService.expirePendingPayments(5L);

        assertThat(a.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(b.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(paymentRepository).saveAll(List.of(a, b));
    }

    @Test
    @DisplayName("cancelDepositPayment: sinal pendente é CANCELADO")
    void cancelsPendingDeposit() {
        Payment deposit = payment(PaymentType.DEPOSIT, PaymentStatus.PENDING);
        when(paymentRepository.findByReservationIdAndType(5L, PaymentType.DEPOSIT)).thenReturn(Optional.of(deposit));

        paymentService.cancelDepositPayment(5L, true);

        assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(deposit);
    }

    @Test
    @DisplayName("cancelDepositPayment: sinal pago é REEMBOLSADO quando elegível e RETIDO quando não")
    void refundsOrRetainsConfirmedDeposit() {
        Payment refundable = payment(PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        when(paymentRepository.findByReservationIdAndType(5L, PaymentType.DEPOSIT)).thenReturn(Optional.of(refundable));
        paymentService.cancelDepositPayment(5L, true);
        assertThat(refundable.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        Payment retained = payment(PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        when(paymentRepository.findByReservationIdAndType(6L, PaymentType.DEPOSIT)).thenReturn(Optional.of(retained));
        paymentService.cancelDepositPayment(6L, false);
        assertThat(retained.getStatus()).isEqualTo(PaymentStatus.RETAINED);
    }

    @Test
    @DisplayName("cancelDepositPayment: sinal inexistente ou em estado final é rejeitado")
    void cancelRejectsInvalidStates() {
        when(paymentRepository.findByReservationIdAndType(1L, PaymentType.DEPOSIT)).thenReturn(Optional.empty());
        when(paymentRepository.findByReservationIdAndType(2L, PaymentType.DEPOSIT))
                .thenReturn(Optional.of(payment(PaymentType.DEPOSIT, PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> paymentService.cancelDepositPayment(1L, true))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> paymentService.cancelDepositPayment(2L, true))
                .isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------- estadia: check-in/check-out/no-show

    @Test
    @DisplayName("getConfirmedDeposit: devolve o sinal confirmado")
    void returnsConfirmedDeposit() {
        Payment deposit = payment(PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        when(paymentRepository.findByReservationIdAndType(5L, PaymentType.DEPOSIT)).thenReturn(Optional.of(deposit));

        assertThat(paymentService.getConfirmedDeposit(5L)).isSameAs(deposit);
    }

    @Test
    @DisplayName("getConfirmedDeposit: recusa sinal inexistente ou não confirmado")
    void confirmedDepositRejections() {
        when(paymentRepository.findByReservationIdAndType(1L, PaymentType.DEPOSIT)).thenReturn(Optional.empty());
        when(paymentRepository.findByReservationIdAndType(2L, PaymentType.DEPOSIT))
                .thenReturn(Optional.of(payment(PaymentType.DEPOSIT, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> paymentService.getConfirmedDeposit(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> paymentService.getConfirmedDeposit(2L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("retainDepositPayment: marca o sinal confirmado como RETIDO")
    void retainsConfirmedDeposit() {
        Payment deposit = payment(PaymentType.DEPOSIT, PaymentStatus.CONFIRMED);
        when(paymentRepository.findByReservationIdAndType(5L, PaymentType.DEPOSIT)).thenReturn(Optional.of(deposit));

        paymentService.retainDepositPayment(5L);

        assertThat(deposit.getStatus()).isEqualTo(PaymentStatus.RETAINED);
        verify(paymentRepository).save(deposit);
    }

    @Test
    @DisplayName("retainDepositPayment: recusa sinal inexistente ou não confirmado")
    void retainRejections() {
        when(paymentRepository.findByReservationIdAndType(1L, PaymentType.DEPOSIT)).thenReturn(Optional.empty());
        when(paymentRepository.findByReservationIdAndType(2L, PaymentType.DEPOSIT))
                .thenReturn(Optional.of(payment(PaymentType.DEPOSIT, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> paymentService.retainDepositPayment(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> paymentService.retainDepositPayment(2L)).isInstanceOf(IllegalStateException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckoutPayment: cria pagamento CHECKOUT pendente com o valor informado")
    void createsCheckoutPayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        Reservation reservation = reservation();

        Payment payment = paymentService.createCheckoutPayment(reservation, new BigDecimal("190.00"));

        assertThat(payment.getType()).isEqualTo(PaymentType.CHECKOUT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAmount()).isEqualByComparingTo("190.00");
        assertThat(payment.getReservation()).isSameAs(reservation);
    }

    @Test
    @DisplayName("createCheckoutPayment: recusa valor nulo, zero ou negativo")
    void checkoutPaymentRejectsInvalidAmounts() {
        Reservation reservation = reservation();

        assertThatThrownBy(() -> paymentService.createCheckoutPayment(reservation, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> paymentService.createCheckoutPayment(reservation, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> paymentService.createCheckoutPayment(reservation, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkoutPaymentExists: consulta o repositório")
    void checkoutPaymentExists() {
        when(paymentRepository.existsByReservationIdAndType(5L, PaymentType.CHECKOUT)).thenReturn(true);
        when(paymentRepository.existsByReservationIdAndType(6L, PaymentType.CHECKOUT)).thenReturn(false);

        assertThat(paymentService.checkoutPaymentExists(5L)).isTrue();
        assertThat(paymentService.checkoutPaymentExists(6L)).isFalse();
    }
}