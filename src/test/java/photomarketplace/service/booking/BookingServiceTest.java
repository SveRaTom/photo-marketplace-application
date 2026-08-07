package photomarketplace.service.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.model.entity.booking.Booking;
import photomarketplace.model.entity.booking.BookingStatus;
import photomarketplace.repository.booking.BookingRepository;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.service.user.UserService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

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
}
