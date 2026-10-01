package com.walkmates.lab2;

import com.walkmates.model.Booking;
import com.walkmates.model.Listing;
import com.walkmates.model.Provider;
import com.walkmates.model.Seeker;
import com.walkmates.repository.BookingRepository;
import com.walkmates.repository.ListingRepository;
import com.walkmates.repository.ProviderRepository;
import com.walkmates.repository.SeekerRepository;
import com.walkmates.service.BookingService;
import com.walkmates.service.PricingCalculator;
import com.walkmates.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyDouble;

class BookingServiceTest {
    private SeekerRepository seekers;
    private ListingRepository listings;
    private ProviderRepository providers;
    private BookingRepository bookings;
    private PricingCalculator pricing;
    private NotificationService notifications;

    private BookingService bookingService;

    private Seeker seeker;
    private Listing listing;
    private Provider provider;

    private final String seekerId = "seeker-1";
    private final String listingId = "listing-1";
    private final String providerId = "provider-1";

    @BeforeEach
    void setUp() {
        seekers = mock(SeekerRepository.class);
        listings = mock(ListingRepository.class);
        providers = mock(ProviderRepository.class);
        bookings = mock(BookingRepository.class);
        pricing = mock(PricingCalculator.class);
        notifications = mock(NotificationService.class);

        bookingService = new BookingService(
                seekers,
                listings,
                providers,
                bookings,
                pricing,
                notifications
        );

        seeker = mock(Seeker.class);
        listing = mock(Listing.class);
        provider = mock(Provider.class);
    }

    @Test
    void createBookingSuccessfullySavesBookingAndSendsNotification() {
        when(seekers.findById(seekerId)).thenReturn(Optional.of(seeker));

        when(listings.findById(listingId)).thenReturn(Optional.of(listing));

        when(listing.isAvailable()).thenReturn(true);

        when(bookings.findBySeekerId(seekerId)).thenReturn(List.of());

        when(listing.getProviderId()).thenReturn(providerId);

        when(providers.findById(providerId)).thenReturn(Optional.of(provider));

        when(listings.findByProviderId(providerId)).thenReturn(List.of(listing));

        when(bookings.findByListingId(listingId)).thenReturn(List.of());

        when(provider.getCapacity()).thenReturn(5);

        when(seeker.getMaxConcurrentBookings()).thenReturn(3);

        when(seeker.getBalance()).thenReturn(1000.0);

        when(pricing.priceFor(any(Booking.class), eq(listing), eq(seeker))).thenReturn(200.0);

        Booking result = bookingService.createBooking(seekerId, listingId, 120);

        assertNotNull(result);

        verify(seeker).charge(200.0);

        verify(seekers).save(seeker);
        verify(listings).save(listing);
        verify(bookings).save(result);

        verify(notifications).sendBookingConfirmed(seeker, result);
    }

    @Test
    void createBookingWithUnknownSeekerThrowsException() {
        when(seekers.findById(seekerId)).thenReturn(Optional.empty());

        assertThrows(
                BookingService.BookingRejectedException.class,
                () -> bookingService.createBooking(seekerId, listingId, 120)
        );

        verifyNoInteractions(listings, providers, pricing, notifications);
    }

    @Test
    void createBookingWithUnknownListingThrowsException() {
        when(seekers.findById(seekerId)).thenReturn(Optional.of(seeker));

        when(listings.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(
                BookingService.BookingRejectedException.class,
                () -> bookingService.createBooking(seekerId, listingId, 120)
        );

        verifyNoInteractions(providers, pricing, notifications);
    }

    @Test
    void createBookingWithUnavailableListingThrowsException() {
        when(seekers.findById(seekerId)).thenReturn(Optional.of(seeker));

        when(listings.findById(listingId)).thenReturn(Optional.of(listing));

        when(listing.isAvailable()).thenReturn(false);

        BookingService.BookingRejectedException exception =
                assertThrows(
                        BookingService.BookingRejectedException.class,
                        () -> bookingService.createBooking(seekerId, listingId, 120)
                );

        assertEquals("Listing is not available", exception.getMessage());

        verifyNoInteractions(pricing, notifications);
        verify(bookings, never()).save(any());
    }

    @Test
    void createBookingWithInsufficientBalanceThrowsException() {
        when(seekers.findById(seekerId)).thenReturn(Optional.of(seeker));

        when(listings.findById(listingId)).thenReturn(Optional.of(listing));

        when(listing.isAvailable()).thenReturn(true);

        when(bookings.findBySeekerId(seekerId)).thenReturn(List.of());

        when(seeker.getMaxConcurrentBookings()).thenReturn(3);

        when(listing.getProviderId()).thenReturn(providerId);

        when(providers.findById(providerId)).thenReturn(Optional.of(provider));

        when(listings.findByProviderId(providerId)).thenReturn(List.of(listing));

        when(bookings.findByListingId(listingId)).thenReturn(List.of());

        when(provider.getCapacity()).thenReturn(5);

        when(seeker.getBalance()).thenReturn(50.0);

        when(pricing.priceFor(any(Booking.class), eq(listing), eq(seeker))).thenReturn(200.0);

        assertThrows(
                BookingService.BookingRejectedException.class,
                () -> bookingService.createBooking(seekerId, listingId, 120)
        );

        verify(seeker, never()).charge(anyDouble());
        verify(seekers, never()).save(any());
        verify(bookings, never()).save(any());
        verify(notifications, never()).sendBookingConfirmed(any(), any());
    }
}