package photomarketplace.web.booking;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.model.dto.booking.BookingRequestDTO;
import photomarketplace.service.booking.BookingService;
import photomarketplace.service.offer.OfferService;

import java.util.UUID;

@Controller
@RequestMapping
public class BookingController {

    private final BookingService bookingService;
    private final OfferService offerService;

    @Autowired
    public BookingController(final BookingService bookingService, final OfferService offerService) {
        this.bookingService = bookingService;
        this.offerService = offerService;
    }

    @GetMapping("/bookings")
    public ModelAndView getBookings(final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("my-bookings");
        modelAndView.addObject("bookings", this.bookingService.getBookingsForUser(getUserId(httpSession)));

        return modelAndView;
    }

    @GetMapping("/my-bookings")
    public ModelAndView getLegacyBookingsPage() {
        return new ModelAndView("redirect:/bookings");
    }

    @GetMapping("/bookings/{id}")
    public ModelAndView getBookingDetails(@PathVariable final UUID id, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("booking-details");
        modelAndView.addObject("booking", this.bookingService.getBookingById(id, getUserId(httpSession)));

        return modelAndView;
    }

    @GetMapping("/bookings/create/{offerId}")
    public ModelAndView getCreateBookingPage(@PathVariable final UUID offerId) {
        final ModelAndView modelAndView = new ModelAndView("create-booking");
        modelAndView.addObject("offerId", offerId);
        modelAndView.addObject("offer", this.offerService.getOfferById(offerId));
        modelAndView.addObject("bookingRequestDTO", BookingRequestDTO.builder().build());

        return modelAndView;
    }

    @PostMapping("/bookings/create/{offerId}")
    public ModelAndView createBooking(@PathVariable final UUID offerId,
                                      @Valid final BookingRequestDTO bookingRequestDTO,
                                      final BindingResult bindingResult,
                                      final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("create-booking");
            modelAndView.addObject("offerId", offerId);
            modelAndView.addObject("offer", this.offerService.getOfferById(offerId));
            modelAndView.addObject("bookingRequestDTO", bookingRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.bookingRequestDTO",
                    bindingResult);

            return modelAndView;
        }

        final UUID bookingId = this.bookingService.createBooking(offerId, bookingRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/bookings/" + bookingId);
    }

    @GetMapping("/bookings/edit/{id}")
    public ModelAndView getEditBookingPage(@PathVariable final UUID id, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("edit-booking");
        modelAndView.addObject("bookingId", id);
        modelAndView.addObject("bookingRequestDTO",
                this.bookingService.getBookingForEdit(id, getUserId(httpSession)));

        return modelAndView;
    }

    @PutMapping("/bookings/edit/{id}")
    public ModelAndView editBooking(@PathVariable final UUID id,
                                    @Valid final BookingRequestDTO bookingRequestDTO,
                                    final BindingResult bindingResult,
                                    final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("edit-booking");
            modelAndView.addObject("bookingId", id);
            modelAndView.addObject("bookingRequestDTO", bookingRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.bookingRequestDTO",
                    bindingResult);

            return modelAndView;
        }

        this.bookingService.updateBooking(id, bookingRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/bookings/" + id);
    }

    @PostMapping("/bookings/delete/{id}")
    public ModelAndView deleteBooking(@PathVariable final UUID id, final HttpSession httpSession) {
        this.bookingService.deleteBooking(id, getUserId(httpSession));

        return new ModelAndView("redirect:/bookings");
    }

    @PostMapping("/bookings/{id}/approve")
    public ModelAndView approveBooking(@PathVariable final UUID id, final HttpSession httpSession) {
        this.bookingService.approveBooking(id, getUserId(httpSession));

        return new ModelAndView("redirect:/bookings/" + id);
    }

    @PostMapping("/bookings/{id}/reject")
    public ModelAndView rejectBooking(@PathVariable final UUID id, final HttpSession httpSession) {
        this.bookingService.rejectBooking(id, getUserId(httpSession));

        return new ModelAndView("redirect:/bookings/" + id);
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute("user_id");
    }
}
