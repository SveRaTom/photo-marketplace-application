package photomarketplace.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.dashboard.PhotographerDashboardDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.review.ReviewService;
import photomarketplace.service.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_PHOTOGRAPHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private UserService userService;

    @Mock
    private OfferService offerService;

    @Mock
    private BookingService bookingService;

    @Mock
    private ReviewService reviewService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        this.dashboardService = new DashboardService(
                this.userService,
                this.offerService,
                this.bookingService,
                this.reviewService
        );
    }

    @Test
    void getPhotographerDashboardShouldAggregateOwnedDomainData() {
        final UserDTO photographer = photographer(PHOTOGRAPHER_ID, UserRole.PHOTOGRAPHER);
        final BookingDTO approvedBooking = booking(
                PHOTOGRAPHER_ID,
                LocalDate.now().plusDays(1),
                BookingStatus.APPROVED
        );
        final BookingDTO pendingBooking = booking(
                PHOTOGRAPHER_ID,
                LocalDate.now().plusDays(2),
                BookingStatus.PENDING
        );
        final BookingDTO pastPendingBooking = booking(
                PHOTOGRAPHER_ID,
                LocalDate.now().minusDays(1),
                BookingStatus.PENDING
        );
        final BookingDTO cancelledBooking = booking(
                PHOTOGRAPHER_ID,
                LocalDate.now().plusDays(3),
                BookingStatus.CANCELLED
        );
        final ReviewDTO newestReview = review(PHOTOGRAPHER_ID, 5, LocalDateTime.now().minusHours(1));
        final ReviewDTO olderReview = review(PHOTOGRAPHER_ID, 3, LocalDateTime.now().minusDays(2));
        final ReviewDTO undatedReview = review(PHOTOGRAPHER_ID, 4, null);

        when(this.userService.getUserById(PHOTOGRAPHER_ID)).thenReturn(photographer);
        when(this.offerService.getOffersByPhotographer(PHOTOGRAPHER_ID)).thenReturn(List.of(
                offer(PHOTOGRAPHER_ID, true),
                offer(PHOTOGRAPHER_ID, true),
                offer(PHOTOGRAPHER_ID, false))
        );
        when(this.bookingService.getBookingsForUser(PHOTOGRAPHER_ID)).thenReturn(List.of(
                pendingBooking,
                approvedBooking,
                pastPendingBooking,
                cancelledBooking,
                booking(OTHER_PHOTOGRAPHER_ID, LocalDate.now().plusDays(1), BookingStatus.PENDING))
        );
        when(this.reviewService.getReviewsForUser(PHOTOGRAPHER_ID)).thenReturn(List.of(
                olderReview,
                newestReview,
                undatedReview,
                review(OTHER_PHOTOGRAPHER_ID, 1, LocalDateTime.now()),
                ReviewDTO.builder().rating(1).createdAt(LocalDateTime.now()).build(),
                ReviewDTO.builder()
                        .rating(1)
                        .offer(OfferDTO.builder().build())
                        .createdAt(LocalDateTime.now())
                        .build())
        );

        final PhotographerDashboardDTO dashboard =
                this.dashboardService.getPhotographerDashboard(PHOTOGRAPHER_ID);

        assertSame(photographer, dashboard.getPhotographer());
        assertEquals(3, dashboard.getTotalOffers());
        assertEquals(2, dashboard.getAvailableOffers());
        assertEquals(4, dashboard.getTotalBookings());
        assertEquals(2, dashboard.getPendingBookings());
        assertEquals(new BigDecimal("4.0"), dashboard.getAverageRating());
        assertEquals(List.of(approvedBooking, pendingBooking), dashboard.getUpcomingBookings());
        assertEquals(List.of(newestReview, olderReview, undatedReview), dashboard.getRecentReviews());
    }

    @Test
    void getPhotographerDashboardShouldReturnEmptyCollectionsAndZeroRating() {
        final UserDTO photographer = photographer(PHOTOGRAPHER_ID, UserRole.PHOTOGRAPHER);

        when(this.userService.getUserById(PHOTOGRAPHER_ID)).thenReturn(photographer);
        when(this.offerService.getOffersByPhotographer(PHOTOGRAPHER_ID)).thenReturn(List.of());
        when(this.bookingService.getBookingsForUser(PHOTOGRAPHER_ID)).thenReturn(List.of());
        when(this.reviewService.getReviewsForUser(PHOTOGRAPHER_ID)).thenReturn(List.of());

        final PhotographerDashboardDTO dashboard =
                this.dashboardService.getPhotographerDashboard(PHOTOGRAPHER_ID);

        assertEquals(0, dashboard.getTotalOffers());
        assertEquals(0, dashboard.getAvailableOffers());
        assertEquals(0, dashboard.getTotalBookings());
        assertEquals(0, dashboard.getPendingBookings());
        assertEquals(new BigDecimal("0.0"), dashboard.getAverageRating());
        assertEquals(List.of(), dashboard.getUpcomingBookings());
        assertEquals(List.of(), dashboard.getRecentReviews());
    }

    @Test
    void getPhotographerDashboardShouldRejectNonPhotographer() {
        when(this.userService.getUserById(PHOTOGRAPHER_ID))
                .thenReturn(photographer(PHOTOGRAPHER_ID, UserRole.CLIENT));

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.dashboardService.getPhotographerDashboard(PHOTOGRAPHER_ID));

        assertEquals("The dashboard is available only to photographers.", exception.getMessage());

        verifyNoInteractions(this.offerService, this.bookingService, this.reviewService);
    }

    private static UserDTO photographer(final UUID photographerId, final UserRole role) {
        return UserDTO.builder()
                .id(photographerId)
                .firstName("Alex")
                .displayName("Alex Studio")
                .role(role)
                .build();
    }

    private static OfferDTO offer(final UUID photographerId, final boolean available) {
        return OfferDTO.builder()
                .id(UUID.randomUUID())
                .isAvailable(available)
                .photographer(photographer(photographerId, UserRole.PHOTOGRAPHER))
                .build();
    }

    private static BookingDTO booking(
            final UUID photographerId,
            final LocalDate eventDate,
            final BookingStatus status) {

        return BookingDTO.builder()
                .id(UUID.randomUUID())
                .photographerId(photographerId)
                .eventDate(eventDate)
                .status(status)
                .build();
    }

    private static ReviewDTO review(
            final UUID photographerId,
            final int rating,
            final LocalDateTime createdAt) {

        return ReviewDTO.builder()
                .id(UUID.randomUUID())
                .rating(rating)
                .offer(offer(photographerId, true))
                .createdAt(createdAt)
                .build();
    }
}
