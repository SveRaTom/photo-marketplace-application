package photomarketplace.service.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.exception.ForbiddenOperationException;
import photomarketplace.exception.InvalidOperationException;
import photomarketplace.exception.ResourceNotFoundException;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.review.ReviewRequestDTO;
import photomarketplace.model.entity.booking.Booking;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.review.Review;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.booking.BookingRepository;
import photomarketplace.repository.review.ReviewRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final UUID REVIEW_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_REVIEW_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CLIENT_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PHOTOGRAPHER_ID =
            UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        this.reviewService = new ReviewService(this.reviewRepository, this.bookingRepository);
    }

    @Test
    void getReviewsForUserShouldMergeDeduplicateSortAndMapPermissions() {
        final Review authoredReview = review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );
        final Review photographerReview = review(
                SECOND_REVIEW_ID,
                OTHER_USER_ID,
                CLIENT_ID,
                null
        );

        when(this.reviewRepository.findAllByAuthorIdOrderByCreatedAtDesc(CLIENT_ID))
                .thenReturn(List.of(authoredReview));
        when(this.reviewRepository.findAllByOfferPhotographerIdOrderByCreatedAtDesc(CLIENT_ID))
                .thenReturn(List.of(photographerReview, authoredReview));

        final List<ReviewDTO> reviews = this.reviewService.getReviewsForUser(CLIENT_ID);

        assertEquals(List.of(REVIEW_ID, SECOND_REVIEW_ID),
                reviews.stream().map(ReviewDTO::getId).toList());
        assertTrue(reviews.getFirst().isCanEdit());
        assertTrue(reviews.getFirst().isCanDelete());
        assertFalse(reviews.getLast().isCanEdit());
        assertFalse(reviews.getLast().isCanDelete());
    }

    @Test
    void getReviewsForOfferShouldMapReviewAsReadOnlyForVisitor() {
        final Review review = review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );

        when(this.reviewRepository.findAllByOfferIdOrderByCreatedAtDesc(OFFER_ID))
                .thenReturn(List.of(review));

        final ReviewDTO result =
                this.reviewService.getReviewsForOffer(OFFER_ID, OTHER_USER_ID).getFirst();

        assertEquals(REVIEW_ID, result.getId());
        assertFalse(result.isCanEdit());
        assertFalse(result.isCanDelete());
    }

    @Test
    void getReviewByIdShouldMapReviewAndDomainSummaries() {
        final Review review = review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );

        when(this.reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        final ReviewDTO result = this.reviewService.getReviewById(REVIEW_ID, CLIENT_ID);

        assertEquals(REVIEW_ID, result.getId());
        assertEquals(5, result.getRating());
        assertEquals("Excellent photography service", result.getComment());
        assertEquals(CLIENT_ID, result.getAuthorId());
        assertEquals(OFFER_ID, result.getOfferId());
        assertEquals(BOOKING_ID, result.getBookingId());
        assertEquals(CLIENT_ID, result.getAuthor().getId());
        assertEquals("Wedding photography", result.getOffer().getTitle());
        assertEquals(BookingStatus.APPROVED, result.getBooking().getStatus());
        assertEquals(PHOTOGRAPHER_ID, result.getBooking().getPhotographerId());
        assertTrue(result.isCanEdit());
        assertTrue(result.isCanDelete());
    }

    @Test
    void getReviewableBookingShouldMapApprovedClientBooking() {
        final Booking booking = booking(BookingStatus.APPROVED, CLIENT_ID, PHOTOGRAPHER_ID);

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final BookingDTO result = this.reviewService.getReviewableBooking(BOOKING_ID, CLIENT_ID);

        assertEquals(BOOKING_ID, result.getId());
        assertEquals(LocalDate.of(2026, 9, 10), result.getEventDate());
        assertEquals("Sofia", result.getLocation());
        assertEquals(CLIENT_ID, result.getClientId());
        assertEquals(PHOTOGRAPHER_ID, result.getPhotographerId());
        assertEquals(OFFER_ID, result.getOfferId());
        assertEquals("Wedding photography", result.getOffer().getTitle());
    }

    @Test
    void getReviewableBookingShouldAcceptCompletedBooking() {
        final Booking booking = booking(BookingStatus.COMPLETED, CLIENT_ID, PHOTOGRAPHER_ID);

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final BookingDTO result = this.reviewService.getReviewableBooking(BOOKING_ID, CLIENT_ID);

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
    }

    @Test
    void getReviewForEditShouldMapOwnedReview() {
        final Review review = review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );

        when(this.reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        final ReviewRequestDTO result = this.reviewService.getReviewForEdit(REVIEW_ID, CLIENT_ID);

        assertEquals(review.getRating(), result.getRating());
        assertEquals(review.getComment(), result.getComment());
    }

    @Test
    void createReviewShouldPersistReviewAndAttachItToBooking() {
        final Booking booking = booking(BookingStatus.APPROVED, CLIENT_ID, PHOTOGRAPHER_ID);
        final ReviewRequestDTO request = reviewRequest(4, "Very good photography service");

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(this.reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            final Review savedReview = invocation.getArgument(0);
            savedReview.setId(REVIEW_ID);

            return savedReview;
        });

        final UUID result = this.reviewService.createReview(BOOKING_ID, request, CLIENT_ID);

        assertEquals(REVIEW_ID, result);

        final ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);

        verify(this.reviewRepository).save(reviewCaptor.capture());

        final Review savedReview = reviewCaptor.getValue();
        assertEquals(4, savedReview.getRating());
        assertEquals("Very good photography service", savedReview.getComment());
        assertSame(booking.getClient(), savedReview.getAuthor());
        assertSame(booking.getOffer(), savedReview.getOffer());
        assertSame(booking, savedReview.getBooking());
        assertSame(savedReview, booking.getReview());
    }

    @Test
    void updateReviewShouldPersistUpdatedValues() {
        final Review review = review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );

        final ReviewRequestDTO request = reviewRequest(3, "Updated review comment");

        when(this.reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        this.reviewService.updateReview(REVIEW_ID, request, CLIENT_ID);

        assertEquals(3, review.getRating());
        assertEquals("Updated review comment", review.getComment());

        verify(this.reviewRepository).save(review);
    }

    @Test
    void deleteReviewShouldDetachItFromBookingAndDeleteIt() {
        final Review review = review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        );

        review.getBooking().setReview(review);

        when(this.reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        this.reviewService.deleteReview(REVIEW_ID, CLIENT_ID);

        assertNull(review.getBooking().getReview());

        verify(this.reviewRepository).delete(review);
    }

    @Test
    void getReviewForEditShouldRejectNonAuthor() {
        when(this.reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review(
                REVIEW_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                LocalDateTime.of(2026, 8, 5, 10, 0)
        )));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.reviewService.getReviewForEdit(REVIEW_ID, OTHER_USER_ID)
        );

        assertEquals("Only the review author can manage this review.", exception.getMessage());

        verify(this.reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void getReviewByIdShouldRejectMissingReview() {
        when(this.reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.reviewService.getReviewById(REVIEW_ID, CLIENT_ID)
        );

        assertEquals("Review with id '%s' does not exist.".formatted(REVIEW_ID),
                exception.getMessage());
    }

    @Test
    void getReviewableBookingShouldRejectNonClient() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(
                booking(BookingStatus.APPROVED, CLIENT_ID, PHOTOGRAPHER_ID)
        ));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.reviewService.getReviewableBooking(BOOKING_ID, OTHER_USER_ID)
        );

        assertEquals("Only the booking client can review this offer.", exception.getMessage());

        verify(this.reviewRepository, never()).existsByBookingId(BOOKING_ID);
    }

    @Test
    void getReviewableBookingShouldRejectPendingBooking() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(
                booking(BookingStatus.PENDING, CLIENT_ID, PHOTOGRAPHER_ID)
        ));

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.reviewService.getReviewableBooking(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("Only approved or completed bookings can be reviewed.",
                exception.getMessage());

        verify(this.reviewRepository, never()).existsByBookingId(BOOKING_ID);
    }

    @Test
    void getReviewableBookingShouldRejectAlreadyReviewedBooking() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(
                booking(BookingStatus.APPROVED, CLIENT_ID, PHOTOGRAPHER_ID)
        ));
        when(this.reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(true);

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.reviewService.getReviewableBooking(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("This booking already has a review.", exception.getMessage());
    }

    @Test
    void getReviewableBookingShouldRejectMissingBooking() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.reviewService.getReviewableBooking(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("Booking with id '%s' does not exist.".formatted(BOOKING_ID),
                exception.getMessage());
    }

    private static Review review(
            final UUID reviewId,
            final UUID authorId,
            final UUID photographerId,
            final LocalDateTime createdAt) {

        final Booking booking = booking(BookingStatus.APPROVED, authorId, photographerId);

        return Review.builder()
                .id(reviewId)
                .rating(5)
                .comment("Excellent photography service")
                .author(booking.getClient())
                .offer(booking.getOffer())
                .booking(booking)
                .createdAt(createdAt)
                .updatedAt(LocalDateTime.of(2026, 8, 6, 10, 0))
                .build();
    }

    private static Booking booking(
            final BookingStatus status,
            final UUID clientId,
            final UUID photographerId) {

        return Booking.builder()
                .id(BOOKING_ID)
                .eventDate(LocalDate.of(2026, 9, 10))
                .location("Sofia")
                .notes("Outdoor session")
                .status(status)
                .client(user(clientId, UserRole.CLIENT))
                .offer(offer(photographerId))
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 2, 10, 0))
                .build();
    }

    private static Offer offer(final UUID photographerId) {
        return Offer.builder()
                .id(OFFER_ID)
                .title("Wedding photography")
                .description("Complete wedding photography coverage")
                .price(new BigDecimal("1200.00"))
                .durationHours(8)
                .location("Sofia")
                .isAvailable(true)
                .photographer(user(photographerId, UserRole.PHOTOGRAPHER))
                .build();
    }

    private static User user(final UUID userId, final UserRole role) {
        final String username = role.name().toLowerCase() + userId.toString().substring(0, 4);

        return User.builder()
                .id(userId)
                .firstName("Alex")
                .lastName("Morgan")
                .username(username)
                .email(username + "@example.com")
                .password("hashedPassword")
                .role(role)
                .isActive(true)
                .build();
    }

    private static ReviewRequestDTO reviewRequest(final int rating, final String comment) {
        return ReviewRequestDTO.builder()
                .rating(rating)
                .comment(comment)
                .build();
    }
}
