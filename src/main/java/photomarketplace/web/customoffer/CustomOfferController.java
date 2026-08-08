package photomarketplace.web.customoffer;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import photomarketplace.exception.customoffer.CustomOfferException;
import photomarketplace.model.dto.customoffer.CustomOfferRequestDTO;
import photomarketplace.security.MarketplaceSession;
import photomarketplace.service.customoffer.CustomOfferService;
import photomarketplace.service.offer.OfferService;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/offers/{offerId}/custom-offer")
@RequiredArgsConstructor
public class CustomOfferController {

    private final CustomOfferService customOfferService;
    private final OfferService offerService;

    @GetMapping
    public ModelAndView getCreateCustomOfferPage(@PathVariable final UUID offerId) {
        return createFormView(offerId, CustomOfferRequestDTO.builder().build());
    }

    @PostMapping
    public ModelAndView createCustomOffer(
            @PathVariable final UUID offerId,
            @Valid @ModelAttribute("customOfferRequestDTO") final CustomOfferRequestDTO request,
            final BindingResult bindingResult,
            final HttpSession httpSession,
            final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = createFormView(offerId, request);
            modelAndView.addObject("org.springframework.validation.BindingResult.customOfferRequestDTO",
                    bindingResult);

            return modelAndView;
        }

        try {
            this.customOfferService.createCustomOffer(offerId, getUserId(httpSession), request);
        } catch (CustomOfferException exception) {
            final ModelAndView modelAndView = createFormView(offerId, request);
            modelAndView.addObject("formError", exception.getMessage());

            return modelAndView;
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Your custom offer request was sent successfully.");

        return new ModelAndView("redirect:/offers/" + offerId);
    }

    private ModelAndView createFormView(final UUID offerId, final CustomOfferRequestDTO request) {
        final ModelAndView modelAndView = new ModelAndView("create-custom-offer");
        modelAndView.addObject("offer", this.offerService.getOfferById(offerId));
        modelAndView.addObject("offerId", offerId);
        modelAndView.addObject("minimumEventDate", LocalDate.now().plusDays(1));
        modelAndView.addObject("customOfferRequestDTO", request);

        return modelAndView;
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute(MarketplaceSession.USER_ID);
    }
}
