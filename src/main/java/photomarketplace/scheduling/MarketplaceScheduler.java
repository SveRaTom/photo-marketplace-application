package photomarketplace.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.offer.OfferService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class MarketplaceScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceScheduler.class);

    private final OfferService offerService;
    private final BookingService bookingService;
    private final Clock clock;
    private final long staleOfferDays;

    public MarketplaceScheduler(
            final OfferService offerService,
            final BookingService bookingService,
            final Clock clock,
            @Value("${scheduling.offer-expiration.stale-days}") final long staleOfferDays) {

        if (staleOfferDays < 1) {
            throw new IllegalArgumentException("Stale offer days must be positive.");
        }

        this.offerService = offerService;
        this.bookingService = bookingService;
        this.clock = clock;
        this.staleOfferDays = staleOfferDays;
    }

    @Scheduled(cron = "${scheduling.offer-expiration.cron}")
    void expireStaleOffers() {
        final LocalDateTime cutoff = LocalDateTime.now(this.clock).minusDays(this.staleOfferDays);
        final int expiredOffers = this.offerService.expireStaleOffers(cutoff);

        LOGGER.info("Scheduled offer expiration completed: {} offer(s) expired before {}.",
                expiredOffers, cutoff);
    }

    @Scheduled(
            fixedDelayString = "${scheduling.booking-completion.fixed-delay-ms}",
            initialDelayString = "${scheduling.booking-completion.initial-delay-ms}"
    )
    void completePastBookings() {
        final LocalDate today = LocalDate.now(this.clock);
        final int completedBookings = this.bookingService.completePastApprovedBookings(today);

        LOGGER.info("Scheduled booking completion finished: {} booking(s) completed before {}.",
                completedBookings, today);
    }
}
