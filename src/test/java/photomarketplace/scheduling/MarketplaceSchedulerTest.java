package photomarketplace.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.offer.OfferService;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketplaceSchedulerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-07T13:30:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private OfferService offerService;

    @Mock
    private BookingService bookingService;

    private MarketplaceScheduler marketplaceScheduler;

    @BeforeEach
    void setUp() {
        this.marketplaceScheduler = new MarketplaceScheduler(
                this.offerService,
                this.bookingService,
                FIXED_CLOCK,
                90
        );
    }

    @Test
    void expireStaleOffersShouldUseClockAndConfiguredAge() {
        this.marketplaceScheduler.expireStaleOffers();

        verify(this.offerService).expireStaleOffers(
                LocalDateTime.of(2026, 5, 9, 13, 30)
        );
    }

    @Test
    void completePastBookingsShouldUseCurrentClockDate() {
        this.marketplaceScheduler.completePastBookings();

        verify(this.bookingService).completePastApprovedBookings(
                LocalDate.of(2026, 8, 7)
        );
    }

    @Test
    void constructorShouldRejectNonPositiveStaleOfferAge() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MarketplaceScheduler(
                        this.offerService,
                        this.bookingService,
                        FIXED_CLOCK,
                        0
                )
        );

        assertEquals("Stale offer days must be positive.", exception.getMessage());
    }

    @Test
    void expireStaleOffersShouldUseCronTrigger() throws NoSuchMethodException {
        final Scheduled scheduled = scheduledAnnotation("expireStaleOffers");

        assertEquals("${scheduling.offer-expiration.cron}", scheduled.cron());
        assertEquals("", scheduled.fixedDelayString());
    }

    @Test
    void completePastBookingsShouldUseFixedDelayTrigger() throws NoSuchMethodException {
        final Scheduled scheduled = scheduledAnnotation("completePastBookings");

        assertEquals("${scheduling.booking-completion.fixed-delay-ms}",
                scheduled.fixedDelayString());
        assertEquals("${scheduling.booking-completion.initial-delay-ms}",
                scheduled.initialDelayString());
        assertEquals("", scheduled.cron());
    }

    private static Scheduled scheduledAnnotation(final String methodName)
            throws NoSuchMethodException {

        final Method method = MarketplaceScheduler.class.getDeclaredMethod(methodName);
        final Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);

        return scheduled;
    }
}
