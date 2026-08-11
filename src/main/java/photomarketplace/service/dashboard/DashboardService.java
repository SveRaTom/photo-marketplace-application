package photomarketplace.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.customoffer.CustomOfferStatusDTO;
import photomarketplace.model.dto.dashboard.PhotographerDashboardDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.customoffer.CustomOfferService;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.review.ReviewService;
import photomarketplace.service.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int UPCOMING_BOOKING_LIMIT = 5;
    private static final int RECENT_REVIEW_LIMIT = 4;

    private final UserService userService;
    private final OfferService offerService;
    private final BookingService bookingService;
    private final CustomOfferService customOfferService;
    private final ReviewService reviewService;

    public PhotographerDashboardDTO getPhotographerDashboard(final UUID photographerId) {
        final UserDTO photographer = this.userService.getUserById(photographerId);

        if (photographer.getRole() != UserRole.PHOTOGRAPHER) {
            throw new IllegalArgumentException("The dashboard is available only to photographers.");
        }

        final List<OfferDTO> offers = this.offerService.getOffersByPhotographer(photographerId);
        final List<BookingDTO> bookings = this.bookingService.getBookingsForUser(photographerId).stream()
                .filter(booking -> photographerId.equals(booking.getPhotographerId()))
                .toList();
        final long pendingCustomOffers = this.customOfferService.getPhotographerCustomOffers(photographerId).stream()
                .filter(customOffer -> customOffer.status() == CustomOfferStatusDTO.PENDING)
                .count();
        final List<ReviewDTO> receivedReviews = this.reviewService.getReviewsForUser(photographerId).stream()
                .filter(review -> review.getOffer() != null
                        && review.getOffer().getPhotographer() != null
                        && photographerId.equals(review.getOffer().getPhotographer().getId()))
                .toList();

        return PhotographerDashboardDTO.builder()
                .photographer(photographer)
                .totalOffers(offers.size())
                .availableOffers(offers.stream().filter(OfferDTO::isAvailable).count())
                .totalBookings(bookings.size())
                .pendingRequests(pendingCustomOffers + bookings.stream()
                        .filter(booking -> booking.getStatus() == BookingStatus.PENDING)
                        .count())
                .averageRating(calculateAverageRating(receivedReviews))
                .upcomingBookings(getUpcomingBookings(bookings))
                .recentReviews(getRecentReviews(receivedReviews))
                .build();
    }

    private static List<BookingDTO> getUpcomingBookings(final List<BookingDTO> bookings) {
        return bookings.stream()
                .filter(booking -> !booking.getEventDate().isBefore(LocalDate.now()))
                .filter(booking -> booking.getStatus() == BookingStatus.PENDING
                        || booking.getStatus() == BookingStatus.APPROVED)
                .sorted(Comparator.comparing(BookingDTO::getEventDate))
                .limit(UPCOMING_BOOKING_LIMIT)
                .toList();
    }

    private static List<ReviewDTO> getRecentReviews(final List<ReviewDTO> reviews) {
        return reviews.stream()
                .sorted(Comparator.comparing(ReviewDTO::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_REVIEW_LIMIT)
                .toList();
    }

    private static BigDecimal calculateAverageRating(final List<ReviewDTO> reviews) {
        if (reviews.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        final double averageRating = reviews.stream()
                .mapToInt(ReviewDTO::getRating)
                .average()
                .orElse(0);

        return BigDecimal.valueOf(averageRating).setScale(1, RoundingMode.HALF_UP);
    }
}
