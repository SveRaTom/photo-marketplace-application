package photomarketplace.web.customoffer;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import photomarketplace.exception.customoffer.CustomOfferException;
import photomarketplace.service.customoffer.CustomOfferService;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ClientCustomOfferController {

    private final CustomOfferService customOfferService;

    @GetMapping("/custom-offers")
    public ModelAndView getClientCustomOffers(final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("my-custom-offers");

        try {
            modelAndView.addObject("customOffers",
                    this.customOfferService.getClientCustomOffers(getUserId(httpSession)));
        } catch (CustomOfferException exception) {
            modelAndView.addObject("customOffers", List.of());
            modelAndView.addObject("errorMessage", exception.getMessage());
        }

        return modelAndView;
    }

    @DeleteMapping("/custom-offers/{customOfferId}")
    public ModelAndView withdrawCustomOffer(
            @PathVariable final UUID customOfferId,
            final HttpSession httpSession,
            final RedirectAttributes redirectAttributes) {

        try {
            this.customOfferService.withdrawCustomOffer(customOfferId, getUserId(httpSession));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your custom offer request was withdrawn successfully.");
        } catch (CustomOfferException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/custom-offers");
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute("user_id");
    }
}
