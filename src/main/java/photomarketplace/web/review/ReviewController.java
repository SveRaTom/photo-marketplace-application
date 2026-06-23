package photomarketplace.web.review;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.model.dto.review.ReviewRequestDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.review.ReviewService;

import java.util.UUID;

@Controller
@RequestMapping
public class ReviewController {

    private final ReviewService reviewService;
    private final OfferService offerService;

    @Autowired
    public ReviewController(final ReviewService reviewService,
                            final OfferService offerService) {

        this.reviewService = reviewService;
        this.offerService = offerService;
    }

    @GetMapping("/reviews")
    public ModelAndView getReviews(final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("my-reviews");
        modelAndView.addObject("reviews", this.reviewService.getReviewsForUser(getUserId(httpSession)));

        return modelAndView;
    }

    @GetMapping("/reviews/{id}")
    public ModelAndView getReviewDetails(@PathVariable final UUID id,
                                         @RequestParam(required = false) final String from,
                                         final HttpSession httpSession) {

        final ModelAndView modelAndView = new ModelAndView("review-details");
        final ReviewDTO review = this.reviewService.getReviewById(id, getOptionalUserId(httpSession));

        modelAndView.addObject("review", review);
        modelAndView.addObject("backUrl", resolveBackUrl(from, review));

        return modelAndView;
    }

    @GetMapping("/offers/{offerId}/reviews")
    public ModelAndView getOfferReviews(@PathVariable final UUID offerId, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("offer-reviews");
        modelAndView.addObject("offer", this.offerService.getOfferById(offerId));
        modelAndView.addObject("reviews", this.reviewService.getReviewsForOffer(offerId, getOptionalUserId(httpSession)));

        return modelAndView;
    }

    @GetMapping("/reviews/create/{bookingId}")
    public ModelAndView getCreateReviewPage(@PathVariable final UUID bookingId, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("create-review");
        modelAndView.addObject("bookingId", bookingId);
        modelAndView.addObject("booking", this.reviewService.getReviewableBooking(bookingId, getUserId(httpSession)));
        modelAndView.addObject("reviewRequestDTO", ReviewRequestDTO.builder().build());

        return modelAndView;
    }

    @PostMapping("/reviews/create/{bookingId}")
    public ModelAndView createReview(@PathVariable final UUID bookingId,
                                     @Valid final ReviewRequestDTO reviewRequestDTO,
                                     final BindingResult bindingResult,
                                     final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("create-review");
            modelAndView.addObject("bookingId", bookingId);
            modelAndView.addObject("booking", this.reviewService.getReviewableBooking(bookingId, getUserId(httpSession)));
            modelAndView.addObject("reviewRequestDTO", reviewRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.reviewRequestDTO", bindingResult);

            return modelAndView;
        }

        final UUID reviewId = this.reviewService.createReview(bookingId, reviewRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/reviews/" + reviewId);
    }

    @GetMapping("/reviews/edit/{id}")
    public ModelAndView getEditReviewPage(@PathVariable final UUID id, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("edit-review");
        modelAndView.addObject("reviewId", id);
        modelAndView.addObject("reviewRequestDTO", this.reviewService.getReviewForEdit(id, getUserId(httpSession)));

        return modelAndView;
    }

    @PostMapping("/reviews/edit/{id}")
    public ModelAndView editReview(@PathVariable final UUID id,
                                   @Valid final ReviewRequestDTO reviewRequestDTO,
                                   final BindingResult bindingResult,
                                   final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("edit-review");
            modelAndView.addObject("reviewId", id);
            modelAndView.addObject("reviewRequestDTO", reviewRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.reviewRequestDTO", bindingResult);

            return modelAndView;
        }

        this.reviewService.updateReview(id, reviewRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/reviews/" + id);
    }

    @PostMapping("/reviews/delete/{id}")
    public ModelAndView deleteReview(@PathVariable final UUID id, final HttpSession httpSession) {
        this.reviewService.deleteReview(id, getUserId(httpSession));

        return new ModelAndView("redirect:/reviews");
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute("user_id");
    }

    private static UUID getOptionalUserId(final HttpSession httpSession) {
        return httpSession == null ? null : (UUID) httpSession.getAttribute("user_id");
    }

    private static String resolveBackUrl(final String from, final ReviewDTO review) {
        if ("bookings".equals(from)) {
            return "/bookings";
        }

        if ("booking-details".equals(from)) {
            return "/bookings/" + review.getBookingId();
        }

        if ("my-reviews".equals(from)) {
            return "/reviews";
        }

        return "/offers/" + review.getOfferId() + "/reviews";
    }
}
