package photomarketplace.service.booking;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.booking.BookingRequestDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.entity.booking.Booking;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.review.Review;
import photomarketplace.model.entity.user.User;
import photomarketplace.repository.booking.BookingRepository;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.service.user.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OfferRepository offerRepository;
    private final UserService userService;

    @Autowired
    public BookingService(final BookingRepository bookingRepository,
                          final OfferRepository offerRepository,
                          final UserService userService) {

        this.bookingRepository = bookingRepository;
        this.offerRepository = offerRepository;
        this.userService = userService;
    }

    public List<BookingDTO> getBookingsForUser(final UUID userId) {
        final List<Booking> bookings = new ArrayList<>();
        bookings.addAll(this.bookingRepository.findAllByClientIdOrderByEventDateAsc(userId));
        bookings.addAll(this.bookingRepository.findAllByOfferPhotographerIdOrderByEventDateAsc(userId));

        final Map<UUID, Booking> uniqueBookings = new LinkedHashMap<>();
        bookings.forEach(booking -> uniqueBookings.put(booking.getId(), booking));

        return uniqueBookings.values().stream()
                .sorted(Comparator
                        .comparing(Booking::getEventDate)
                        .thenComparing(booking -> booking.getCreatedAt() == null ? LocalDateTime.MIN : booking.getCreatedAt()))
                .map(booking -> toBookingDTO(booking, userId))
                .toList();
    }

    public BookingDTO getBookingById(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getVisibleBooking(bookingId, currentUserId);

        return toBookingDTO(booking, currentUserId);
    }

    public BookingRequestDTO getBookingForEdit(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getClientPendingBooking(bookingId, currentUserId);

        return BookingRequestDTO.builder()
                .eventDate(booking.getEventDate())
                .location(booking.getLocation())
                .notes(booking.getNotes())
                .build();
    }

    public UUID createBooking(final UUID offerId,
                              final BookingRequestDTO bookingRequest,
                              final UUID clientId) {

        final Offer offer = getOffer(offerId);

        if (!offer.isAvailable()) {
            throw new RuntimeException("This offer is not available for booking.");
        }

        if (offer.getPhotographer().getId().equals(clientId)) {
            throw new RuntimeException("You cannot book your own photography offer.");
        }

        final User client = this.userService.getUser(clientId);

        final Booking booking = Booking.builder()
                .eventDate(bookingRequest.getEventDate())
                .location(bookingRequest.getLocation())
                .notes(bookingRequest.getNotes())
                .status(BookingStatus.PENDING)
                .client(client)
                .offer(offer)
                .build();

        return this.bookingRepository.save(booking).getId();
    }

    public void updateBooking(final UUID bookingId,
                              final BookingRequestDTO bookingRequest,
                              final UUID currentUserId) {

        final Booking booking = getClientPendingBooking(bookingId, currentUserId);

        booking.setEventDate(bookingRequest.getEventDate());
        booking.setLocation(bookingRequest.getLocation());
        booking.setNotes(bookingRequest.getNotes());

        this.bookingRepository.save(booking);
    }

    public void deleteBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getClientCancellableBooking(bookingId, currentUserId);

        booking.setStatus(BookingStatus.CANCELLED);
        this.bookingRepository.save(booking);
    }

    public void approveBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getPhotographerPendingBooking(bookingId, currentUserId);

        booking.setStatus(BookingStatus.APPROVED);
        this.bookingRepository.save(booking);
    }

    public void rejectBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getPhotographerPendingBooking(bookingId, currentUserId);

        booking.setStatus(BookingStatus.REJECTED);
        this.bookingRepository.save(booking);
    }

    private Booking getVisibleBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getBooking(bookingId);

        if (!isClient(booking, currentUserId) && !isOfferPhotographer(booking, currentUserId)) {
            throw new RuntimeException("You do not have permission to view this booking.");
        }

        return booking;
    }

    private Booking getClientPendingBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getVisibleBooking(bookingId, currentUserId);

        if (!isClient(booking, currentUserId)) {
            throw new RuntimeException("Only the client who created this booking can manage it.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be changed.");
        }

        return booking;
    }

    private Booking getPhotographerPendingBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getVisibleBooking(bookingId, currentUserId);

        if (!isOfferPhotographer(booking, currentUserId)) {
            throw new RuntimeException("Only the offer photographer can manage this booking request.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be approved or rejected.");
        }

        return booking;
    }

    private Booking getClientCancellableBooking(final UUID bookingId, final UUID currentUserId) {
        final Booking booking = getVisibleBooking(bookingId, currentUserId);

        if (!isClient(booking, currentUserId)) {
            throw new RuntimeException("Only the client who created this booking can cancel it.");
        }

        if (!isCancellable(booking)) {
            throw new RuntimeException("Only pending or approved bookings can be cancelled.");
        }

        return booking;
    }

    private Booking getBooking(final UUID bookingId) {
        return this.bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking with id [%s] does not exist.".formatted(bookingId)));
    }

    private Offer getOffer(final UUID offerId) {
        return this.offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer with id [%s] does not exist.".formatted(offerId)));
    }

    private BookingDTO toBookingDTO(final Booking booking, final UUID currentUserId) {
        final User client = booking.getClient();
        final Offer offer = booking.getOffer();
        final User photographer = offer.getPhotographer();
        final boolean clientOwner = isClient(booking, currentUserId);
        final boolean photographerOwner = isOfferPhotographer(booking, currentUserId);
        final boolean pending = booking.getStatus() == BookingStatus.PENDING;
        final boolean cancellable = isCancellable(booking);
        final boolean reviewable = isReviewable(booking);

        return BookingDTO.builder()
                .id(booking.getId())
                .eventDate(booking.getEventDate())
                .location(booking.getLocation())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .clientId(client.getId())
                .photographerId(photographer.getId())
                .offerId(offer.getId())
                .client(UserMapper.toUserDTO(client))
                .offer(toOfferSummaryDTO(offer))
                .review(toReviewSummaryDTO(booking.getReview()))
                .canEdit(clientOwner && pending)
                .canCancel(clientOwner && cancellable)
                .canApprove(photographerOwner && pending)
                .canReject(photographerOwner && pending)
                .canReview(clientOwner && reviewable && booking.getReview() == null)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
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

    private ReviewDTO toReviewSummaryDTO(final Review review) {
        if (review == null) {
            return null;
        }

        return ReviewDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .authorId(review.getAuthor().getId())
                .offerId(review.getOffer().getId())
                .bookingId(review.getBooking().getId())
                .author(UserMapper.toUserDTO(review.getAuthor()))
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private static boolean isClient(final Booking booking, final UUID currentUserId) {
        return booking.getClient().getId().equals(currentUserId);
    }

    private static boolean isOfferPhotographer(final Booking booking, final UUID currentUserId) {
        return booking.getOffer().getPhotographer().getId().equals(currentUserId);
    }

    private static boolean isCancellable(final Booking booking) {
        return booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.APPROVED;
    }

    private static boolean isReviewable(final Booking booking) {
        return booking.getStatus() == BookingStatus.APPROVED || booking.getStatus() == BookingStatus.COMPLETED;
    }
}
