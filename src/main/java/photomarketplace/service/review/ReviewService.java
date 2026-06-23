package photomarketplace.service.review;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.review.ReviewRequestDTO;
import photomarketplace.model.entity.booking.Booking;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.review.Review;
import photomarketplace.model.entity.user.User;
import photomarketplace.repository.booking.BookingRepository;
import photomarketplace.repository.review.ReviewRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public ReviewService(final ReviewRepository reviewRepository,
                         final BookingRepository bookingRepository) {

        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<ReviewDTO> getReviewsForUser(final UUID currentUserId) {
        final List<Review> reviews = new ArrayList<>();
        reviews.addAll(this.reviewRepository.findAllByAuthorIdOrderByCreatedAtDesc(currentUserId));
        reviews.addAll(this.reviewRepository.findAllByOfferPhotographerIdOrderByCreatedAtDesc(currentUserId));

        final Map<UUID, Review> uniqueReviews = new LinkedHashMap<>();
        reviews.forEach(review -> uniqueReviews.put(review.getId(), review));

        return uniqueReviews.values().stream()
                .sorted(Comparator
                        .comparing((Review review) -> review.getCreatedAt() == null
                                ? LocalDateTime.MIN
                                : review.getCreatedAt())
                        .reversed())
                .map(review -> toReviewDTO(review, currentUserId))
                .toList();
    }

    public List<ReviewDTO> getReviewsForOffer(final UUID offerId, final UUID currentUserId) {
        return this.reviewRepository.findAllByOfferIdOrderByCreatedAtDesc(offerId).stream()
                .map(review -> toReviewDTO(review, currentUserId))
                .toList();
    }

    public ReviewDTO getReviewById(final UUID reviewId, final UUID currentUserId) {
        final Review review = getReview(reviewId);

        return toReviewDTO(review, currentUserId);
    }

    public BookingDTO getReviewableBooking(final UUID bookingId, final UUID currentUserId) {
        return toBookingSummaryDTO(getBookingEligibleForReview(bookingId, currentUserId));
    }

    public ReviewRequestDTO getReviewForEdit(final UUID reviewId, final UUID currentUserId) {
        final Review review = getOwnedReview(reviewId, currentUserId);

        return ReviewRequestDTO.builder()
                .rating(review.getRating())
                .comment(review.getComment())
                .build();
    }

    public UUID createReview(final UUID bookingId,
                             final ReviewRequestDTO reviewRequest,
                             final UUID currentUserId) {

        final Booking booking = getBookingEligibleForReview(bookingId, currentUserId);
        final Review review = Review.builder()
                .rating(reviewRequest.getRating())
                .comment(reviewRequest.getComment())
                .author(booking.getClient())
                .offer(booking.getOffer())
                .booking(booking)
                .build();

        final Review savedReview = this.reviewRepository.save(review);
        booking.setReview(savedReview);

        return savedReview.getId();
    }

    public void updateReview(final UUID reviewId,
                             final ReviewRequestDTO reviewRequest,
                             final UUID currentUserId) {

        final Review review = getOwnedReview(reviewId, currentUserId);

        review.setRating(reviewRequest.getRating());
        review.setComment(reviewRequest.getComment());

        this.reviewRepository.save(review);
    }

    public void deleteReview(final UUID reviewId, final UUID currentUserId) {
        final Review review = getOwnedReview(reviewId, currentUserId);

        review.getBooking().setReview(null);
        this.reviewRepository.delete(review);
    }

    private Review getOwnedReview(final UUID reviewId, final UUID currentUserId) {
        final Review review = getReview(reviewId);

        if (!isAuthor(review, currentUserId)) {
            throw new RuntimeException("Only the review author can manage this review.");
        }

        return review;
    }

    private Booking getBookingEligibleForReview(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getBooking(bookingId);

        if (!booking.getClient().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the booking client can review this offer.");
        }

        if (!isReviewable(booking)) {
            throw new RuntimeException("Only approved or completed bookings can be reviewed.");
        }

        if (this.reviewRepository.existsByBookingId(bookingId)) {
            throw new RuntimeException("This booking already has a review.");
        }

        return booking;
    }

    private Review getReview(final UUID reviewId) {
        return this.reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review with id [%s] does not exist.".formatted(reviewId)));
    }

    private Booking getBooking(final UUID bookingId) {
        return this.bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking with id [%s] does not exist.".formatted(bookingId)));
    }

    private ReviewDTO toReviewDTO(final Review review, final UUID currentUserId) {
        final User author = review.getAuthor();
        final Offer offer = review.getOffer();
        final Booking booking = review.getBooking();

        return ReviewDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .authorId(author.getId())
                .offerId(offer.getId())
                .bookingId(booking.getId())
                .author(UserMapper.toUserDTO(author))
                .offer(toOfferSummaryDTO(offer))
                .booking(toBookingSummaryDTO(booking))
                .canEdit(isAuthor(review, currentUserId))
                .canDelete(isAuthor(review, currentUserId))
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private OfferDTO toOfferSummaryDTO(final Offer offer) {
        return OfferDTO.builder()
                .id(offer.getId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .price(offer.getPrice())
                .durationHours(offer.getDurationHours())
                .location(offer.getLocation())
                .isAvailable(offer.isAvailable())
                .photographer(UserMapper.toUserDTO(offer.getPhotographer()))
                .build();
    }

    private BookingDTO toBookingSummaryDTO(final Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .eventDate(booking.getEventDate())
                .location(booking.getLocation())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .clientId(booking.getClient().getId())
                .photographerId(booking.getOffer().getPhotographer().getId())
                .offerId(booking.getOffer().getId())
                .client(UserMapper.toUserDTO(booking.getClient()))
                .offer(toOfferSummaryDTO(booking.getOffer()))
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private static boolean isAuthor(final Review review, final UUID currentUserId) {
        return review.getAuthor().getId().equals(currentUserId);
    }

    private static boolean isReviewable(final Booking booking) {
        return booking.getStatus() == BookingStatus.APPROVED || booking.getStatus() == BookingStatus.COMPLETED;
    }
}
