package photomarketplace.web.customoffer;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import photomarketplace.exception.customoffer.CustomOfferException;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionDTO;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.security.MarketplaceSession;
import photomarketplace.service.customoffer.CustomOfferService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/photographer/custom-offers")
@RequiredArgsConstructor
public class PhotographerCustomOfferController {

    private final CustomOfferService customOfferService;

    @GetMapping
    public ModelAndView getPhotographerCustomOffers(final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("photographer-custom-offers");

        try {
            modelAndView.addObject(
                    "customOffers",
                    this.customOfferService.getPhotographerCustomOfferViews(getUserId(httpSession)));
        } catch (CustomOfferException exception) {
            modelAndView.addObject("customOffers", List.of());
            modelAndView.addObject("errorMessage", exception.getMessage());
        }

        return modelAndView;
    }

    @PutMapping("/{customOfferId}")
    public ModelAndView decideCustomOffer(
            @PathVariable final UUID customOfferId,
            @Valid @ModelAttribute final CustomOfferDecisionRequestDTO decisionRequest,
            final BindingResult bindingResult,
            final HttpSession httpSession,
            final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());

            return redirectToCustomOffers();
        }

        try {
            this.customOfferService.decideCustomOffer(
                    customOfferId,
                    getUserId(httpSession),
                    decisionRequest);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    decisionRequest.decision() == CustomOfferDecisionDTO.ACCEPT
                            ? "The custom offer was accepted successfully."
                            : "The custom offer was declined successfully.");
        } catch (CustomOfferException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return redirectToCustomOffers();
    }

    private static ModelAndView redirectToCustomOffers() {
        return new ModelAndView("redirect:/photographer/custom-offers");
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute(MarketplaceSession.USER_ID);
    }
}
