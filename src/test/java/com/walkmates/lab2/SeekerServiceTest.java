package com.walkmates.lab2;

import com.walkmates.model.Seeker;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.NotificationService;
import com.walkmates.service.PaymentService;
import com.walkmates.service.SeekerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;

class SeekerServiceTest {
    private SeekerRepository seekers;
    private PaymentService payments;
    private NotificationService notifications;

    private SeekerService seekerService;

    private Seeker seeker;

    private final String seekerId = "seeker-1";
    private final String paymentMethodId = "payment-method-1";

    @BeforeEach
    void setUp() {
        seekers = mock(SeekerRepository.class);
        payments = mock(PaymentService.class);
        notifications = mock(NotificationService.class);

        seekerService = new SeekerService(
                seekers,
                payments,
                notifications
        );

        seeker = mock(Seeker.class);

        when(seekers.findById(seekerId)).thenReturn(Optional.of(seeker));
    }

    @Test
    void topUpSuccessfullyChargesAndCreditsWallet() throws PaymentService.PaymentException {
        double amount = 200.0;

        when(payments.charge(seekerId, paymentMethodId, amount)).thenReturn("payment-confirmation-1");

        when(seekers.save(seeker)).thenReturn(seeker);

        Seeker result = seekerService.topUp(seekerId, paymentMethodId, amount);

        assertSame(seeker, result);

        verify(payments).charge(seekerId, paymentMethodId, amount);

        verify(seeker).addFunds(amount);
        verify(seekers).save(seeker);
    }

    @Test
    void topUpWhenPaymentIsDeclinedDoesNotCreditWallet() throws PaymentService.PaymentException {
        double amount = 200.0;

        when(payments.charge(seekerId, paymentMethodId, amount))
                .thenThrow(new PaymentService.PaymentException("Payment declined"));

        PaymentService.PaymentException exception = assertThrows(
                PaymentService.PaymentException.class,
                () -> seekerService.topUp(seekerId, paymentMethodId, amount)
        );

        assertEquals("Payment declined", exception.getMessage());

        verify(payments).charge(seekerId, paymentMethodId, amount);

        verify(seeker, never()).addFunds(anyDouble());
        verify(seekers, never()).save(any());
    }

    @Test
    void topUpWhenPaymentTimesOutDoesNotCreditWallet() throws PaymentService.PaymentException {
        double amount = 200.0;

        when(payments.charge(seekerId, paymentMethodId, amount))
                .thenThrow(new PaymentService.PaymentTimeoutException("Payment gateway timed out"));

        PaymentService.PaymentTimeoutException exception = assertThrows(
                PaymentService.PaymentTimeoutException.class,
                () -> seekerService.topUp(seekerId, paymentMethodId, amount)
        );

        assertEquals("Payment gateway timed out", exception.getMessage());

        verify(payments).charge(seekerId, paymentMethodId, amount);

        verify(seeker, never()).addFunds(anyDouble());
        verify(seekers, never()).save(any());
    }
}
