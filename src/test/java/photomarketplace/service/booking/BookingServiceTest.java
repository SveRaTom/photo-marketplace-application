package photomarketplace.service.booking;

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
import photomarketplace.model.dto.booking.BookingRequestDTO;
import photomarketplace.model.entity.booking.Booking;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.review.Review;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.booking.BookingRepository;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.service.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final UUID BOOKING_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_BOOKING_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OFFER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLIENT_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PHOTOGRAPHER_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID REVIEW_ID =
            UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 9, 10);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private UserService userService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        this.bookingService = new BookingService(
                this.bookingRepository,
                this.offerRepository,
                this.userService
        );
    }

    @Test
    void getBookingsForUserShouldMergeDeduplicateSortAndMapPermissions() {
        final Booking clientBooking = booking(
                BOOKING_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                BookingStatus.APPROVED,
                EVENT_DATE,
                null
        );
        final Booking photographerBooking = booking(
                SECOND_BOOKING_ID,
                OTHER_USER_ID,
                CLIENT_ID,
                BookingStatus.PENDING,
                EVENT_DATE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );

        when(this.bookingRepository.findAllByClientIdOrderByEventDateAsc(CLIENT_ID))
                .thenReturn(List.of(clientBooking));
        when(this.bookingRepository.findAllByOfferPhotographerIdOrderByEventDateAsc(CLIENT_ID))
                .thenReturn(List.of(photographerBooking, clientBooking));

        final List<BookingDTO> bookings = this.bookingService.getBookingsForUser(CLIENT_ID);

        assertEquals(List.of(BOOKING_ID, SECOND_BOOKING_ID),
                bookings.stream().map(BookingDTO::getId).toList());

        final BookingDTO clientBookingDTO = bookings.getFirst();
        assertTrue(clientBookingDTO.isCanCancel());
        assertTrue(clientBookingDTO.isCanReview());
        assertFalse(clientBookingDTO.isCanEdit());
        assertFalse(clientBookingDTO.isCanApprove());

        final BookingDTO photographerBookingDTO = bookings.getLast();
        assertTrue(photographerBookingDTO.isCanApprove());
        assertTrue(photographerBookingDTO.isCanReject());
        assertFalse(photographerBookingDTO.isCanCancel());
        assertEquals("Portrait session", photographerBookingDTO.getOffer().getTitle());
        assertEquals(OTHER_USER_ID, photographerBookingDTO.getClient().getId());
    }

    @Test
    void getBookingByIdShouldMapExistingReview() {
        final Booking booking = booking(
                BOOKING_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                BookingStatus.COMPLETED,
                EVENT_DATE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        final Review review = Review.builder()
                .id(REVIEW_ID)
                .rating(5)
                .comment("Excellent photography service")
                .author(booking.getClient())
                .offer(booking.getOffer())
                .booking(booking)
                .createdAt(LocalDateTime.of(2026, 9, 11, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 9, 11, 11, 0))
                .build();

        booking.setReview(review);

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final BookingDTO result = this.bookingService.getBookingById(BOOKING_ID, CLIENT_ID);

        assertNotNull(result.getReview());
        assertEquals(REVIEW_ID, result.getReview().getId());
        assertEquals(5, result.getReview().getRating());
        assertEquals(CLIENT_ID, result.getReview().getAuthorId());
        assertEquals(OFFER_ID, result.getReview().getOfferId());
        assertEquals(BOOKING_ID, result.getReview().getBookingId());
        assertFalse(result.isCanReview());
    }

    @Test
    void getBookingByIdShouldRejectUnrelatedUser() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingBooking()));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.bookingService.getBookingById(BOOKING_ID, OTHER_USER_ID)
        );

        assertEquals("You do not have permission to view this booking.", exception.getMessage());
    }

    @Test
    void getBookingByIdShouldRejectMissingBooking() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.bookingService.getBookingById(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("Booking with id '%s' does not exist.".formatted(BOOKING_ID),
                exception.getMessage());
    }

    @Test
    void getBookingForEditShouldMapPendingClientBooking() {
        final Booking booking = pendingBooking();

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final BookingRequestDTO result =
                this.bookingService.getBookingForEdit(BOOKING_ID, CLIENT_ID);

        assertEquals(EVENT_DATE, result.getEventDate());
        assertEquals("Sofia", result.getLocation());
        assertEquals("Outdoor session", result.getNotes());
    }

    @Test
    void getBookingForEditShouldRejectPhotographer() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingBooking()));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.bookingService.getBookingForEdit(BOOKING_ID, PHOTOGRAPHER_ID)
        );

        assertEquals("Only the client who created this booking can manage it.",
                exception.getMessage());
    }

    @Test
    void getBookingForEditShouldRejectNonPendingBooking() {
        final Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.APPROVED);
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.bookingService.getBookingForEdit(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("Only pending bookings can be changed.", exception.getMessage());
    }

    @Test
    void createBookingShouldPersistPendingBooking() {
        final Offer offer = offer(PHOTOGRAPHER_ID, true);
        final User client = user(CLIENT_ID, UserRole.CLIENT);
        final BookingRequestDTO request = bookingRequest(
                LocalDate.of(2026, 10, 1),
                "Plovdiv",
                "Evening session"
        );

        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));
        when(this.userService.getUser(CLIENT_ID)).thenReturn(client);
        when(this.bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            final Booking savedBooking = invocation.getArgument(0);
            savedBooking.setId(BOOKING_ID);

            return savedBooking;
        });

        final UUID result = this.bookingService.createBooking(OFFER_ID, request, CLIENT_ID);

        assertEquals(BOOKING_ID, result);

        final ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);

        verify(this.bookingRepository).save(bookingCaptor.capture());

        final Booking savedBooking = bookingCaptor.getValue();
        assertEquals(request.getEventDate(), savedBooking.getEventDate());
        assertEquals(request.getLocation(), savedBooking.getLocation());
        assertEquals(request.getNotes(), savedBooking.getNotes());
        assertEquals(BookingStatus.PENDING, savedBooking.getStatus());
        assertSame(client, savedBooking.getClient());
        assertSame(offer, savedBooking.getOffer());
    }

    @Test
    void createBookingShouldRejectUnavailableOffer() {
        when(this.offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(offer(PHOTOGRAPHER_ID, false)));

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.bookingService.createBooking(
                        OFFER_ID,
                        bookingRequest(EVENT_DATE, "Sofia", null),
                        CLIENT_ID
                )
        );

        assertEquals("This offer is not available for booking.", exception.getMessage());

        verifyNoInteractions(this.userService);
        verify(this.bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBookingShouldRejectPhotographerBookingOwnOffer() {
        when(this.offerRepository.findById(OFFER_ID))
                .thenReturn(Optional.of(offer(PHOTOGRAPHER_ID, true)));

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.bookingService.createBooking(
                        OFFER_ID,
                        bookingRequest(EVENT_DATE, "Sofia", null),
                        PHOTOGRAPHER_ID
                )
        );

        assertEquals("You cannot book your own photography offer.", exception.getMessage());

        verifyNoInteractions(this.userService);
        verify(this.bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBookingShouldRejectMissingOffer() {
        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.empty());

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.bookingService.createBooking(
                        OFFER_ID,
                        bookingRequest(EVENT_DATE, "Sofia", null),
                        CLIENT_ID
                )
        );

        assertEquals("Offer with id '%s' does not exist.".formatted(OFFER_ID),
                exception.getMessage());
    }

    @Test
    void updateBookingShouldPersistUpdatedFields() {
        final Booking booking = pendingBooking();
        final BookingRequestDTO request = bookingRequest(
                LocalDate.of(2026, 10, 2),
                "Varna",
                "Beach session"
        );

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        this.bookingService.updateBooking(BOOKING_ID, request, CLIENT_ID);

        assertEquals(request.getEventDate(), booking.getEventDate());
        assertEquals(request.getLocation(), booking.getLocation());
        assertEquals(request.getNotes(), booking.getNotes());

        verify(this.bookingRepository).save(booking);
    }

    @Test
    void deleteBookingShouldCancelClientBooking() {
        final Booking booking = pendingBooking();

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        this.bookingService.deleteBooking(BOOKING_ID, CLIENT_ID);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        verify(this.bookingRepository).save(booking);
    }

    @Test
    void deleteBookingShouldRejectPhotographer() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingBooking()));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.bookingService.deleteBooking(BOOKING_ID, PHOTOGRAPHER_ID)
        );

        assertEquals("Only the client who created this booking can cancel it.",
                exception.getMessage());
    }

    @Test
    void deleteBookingShouldRejectCompletedBooking() {
        final Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.COMPLETED);

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.bookingService.deleteBooking(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("Only pending or approved bookings can be cancelled.",
                exception.getMessage());
    }

    @Test
    void approveBookingShouldPersistApprovedStatus() {
        final Booking booking = pendingBooking();

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        this.bookingService.approveBooking(BOOKING_ID, PHOTOGRAPHER_ID);

        assertEquals(BookingStatus.APPROVED, booking.getStatus());

        verify(this.bookingRepository).save(booking);
    }

    @Test
    void rejectBookingShouldPersistRejectedStatus() {
        final Booking booking = pendingBooking();

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        this.bookingService.rejectBooking(BOOKING_ID, PHOTOGRAPHER_ID);

        assertEquals(BookingStatus.REJECTED, booking.getStatus());

        verify(this.bookingRepository).save(booking);
    }

    @Test
    void approveBookingShouldRejectClient() {
        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingBooking()));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.bookingService.approveBooking(BOOKING_ID, CLIENT_ID)
        );

        assertEquals("Only the offer photographer can manage this booking request.",
                exception.getMessage());
    }

    @Test
    void approveBookingShouldRejectNonPendingBooking() {
        final Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.APPROVED);

        when(this.bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        final InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> this.bookingService.approveBooking(BOOKING_ID, PHOTOGRAPHER_ID)
        );

        assertEquals("Only pending bookings can be approved or rejected.",
                exception.getMessage());
    }

    @Test
    void completePastApprovedBookingsShouldCompleteAndPersistBookings() {
        final LocalDate today = LocalDate.of(2026, 8, 7);
        final Booking firstBooking = Booking.builder().status(BookingStatus.APPROVED).build();
        final Booking secondBooking = Booking.builder().status(BookingStatus.APPROVED).build();
        final List<Booking> pastApprovedBookings = List.of(firstBooking, secondBooking);

        when(this.bookingRepository.findAllByStatusAndEventDateBefore(BookingStatus.APPROVED, today))
                .thenReturn(pastApprovedBookings);

        final int completedBookings = this.bookingService.completePastApprovedBookings(today);

        assertEquals(2, completedBookings);
        assertEquals(BookingStatus.COMPLETED, firstBooking.getStatus());
        assertEquals(BookingStatus.COMPLETED, secondBooking.getStatus());

        verify(this.bookingRepository).saveAll(pastApprovedBookings);
    }

    @Test
    void completePastApprovedBookingsShouldNotPersistWhenNoBookingsQualify() {
        final LocalDate today = LocalDate.of(2026, 8, 7);

        when(this.bookingRepository.findAllByStatusAndEventDateBefore(BookingStatus.APPROVED, today))
                .thenReturn(List.of());

        final int completedBookings = this.bookingService.completePastApprovedBookings(today);

        assertEquals(0, completedBookings);

        verify(this.bookingRepository, never()).saveAll(anyList());
    }

    @Test
    void completePastApprovedBookingsShouldRejectMissingDate() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.bookingService.completePastApprovedBookings(null)
        );

        assertEquals("Booking completion date is required.", exception.getMessage());

        verifyNoInteractions(this.bookingRepository);
    }

    private static Booking pendingBooking() {
        return booking(
                BOOKING_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                BookingStatus.PENDING,
                EVENT_DATE,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
    }

    private static Booking booking(
            final UUID bookingId,
            final UUID clientId,
            final UUID photographerId,
            final BookingStatus status,
            final LocalDate eventDate,
            final LocalDateTime createdAt) {

        return Booking.builder()
                .id(bookingId)
                .eventDate(eventDate)
                .location("Sofia")
                .notes("Outdoor session")
                .status(status)
                .client(user(clientId, UserRole.CLIENT))
                .offer(offer(photographerId, true))
                .createdAt(createdAt)
                .updatedAt(LocalDateTime.of(2026, 8, 2, 10, 0))
                .build();
    }

    private static Offer offer(final UUID photographerId, final boolean available) {
        return Offer.builder()
                .id(OFFER_ID)
                .title("Portrait session")
                .description("Professional portrait photography session")
                .price(new BigDecimal("250.00"))
                .durationHours(2)
                .location("Sofia")
                .isAvailable(available)
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

    private static BookingRequestDTO bookingRequest(
            final LocalDate eventDate,
            final String location,
            final String notes) {

        return BookingRequestDTO.builder()
                .eventDate(eventDate)
                .location(location)
                .notes(notes)
                .build();
    }
}
