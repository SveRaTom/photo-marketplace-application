package photomarketplace.service;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.photo.PhotoService;
import photomarketplace.service.review.ReviewService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ServiceValidationIntegrationTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private ReviewService reviewService;

    @Test
    void offerServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.offerService.createOffer(null, UUID.randomUUID())
        );
    }

    @Test
    void bookingServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.bookingService.createBooking(UUID.randomUUID(), null, UUID.randomUUID())
        );
    }

    @Test
    void photoServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.photoService.createPhoto(UUID.randomUUID(), null, UUID.randomUUID())
        );
    }

    @Test
    void reviewServiceShouldRejectNullRequest() {
        assertThrows(
                ConstraintViolationException.class,
                () -> this.reviewService.createReview(UUID.randomUUID(), null, UUID.randomUUID())
        );
    }
}
