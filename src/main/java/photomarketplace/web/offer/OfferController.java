package photomarketplace.web.offer;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.model.dto.offer.OfferRequestDTO;
import photomarketplace.service.offer.OfferService;

import java.util.UUID;

@Controller
@RequestMapping
public class OfferController {

    private final OfferService offerService;

    @Autowired
    public OfferController(final OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping("/offers")
    public ModelAndView getAllOffers() {
        final ModelAndView modelAndView = new ModelAndView("offers");
        modelAndView.addObject("offers", this.offerService.getAllOffers());

        return modelAndView;
    }

    @GetMapping("/offers/{id}")
    public ModelAndView getOfferDetails(@PathVariable final UUID id) {
        final ModelAndView modelAndView = new ModelAndView("offer-details");
        modelAndView.addObject("offer", this.offerService.getOfferById(id));

        return modelAndView;
    }

    @GetMapping("/my-offers")
    public ModelAndView getMyOffers(final HttpSession httpSession) {
        final UUID userId = getUserId(httpSession);

        final ModelAndView modelAndView = new ModelAndView("my-offers");
        modelAndView.addObject("offers", this.offerService.getOffersByPhotographer(userId));

        return modelAndView;
    }

    @GetMapping("/offers/create")
    public ModelAndView getCreateOfferPage() {
        final ModelAndView modelAndView = new ModelAndView("create-offer");
        modelAndView.addObject("offerRequestDTO", OfferRequestDTO.builder().build());

        return modelAndView;
    }

    @PostMapping("/offers/create")
    public ModelAndView createOffer(@Valid final OfferRequestDTO offerRequestDTO,
                                    final BindingResult bindingResult,
                                    final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("create-offer");
            modelAndView.addObject("offerRequestDTO", offerRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.offerRequestDTO", bindingResult);

            return modelAndView;
        }

        final UUID offerId = this.offerService.createOffer(offerRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/offers/" + offerId);
    }

    @GetMapping("/offers/edit/{id}")
    public ModelAndView getEditOfferPage(@PathVariable final UUID id,
                                         final HttpSession httpSession) {

        final ModelAndView modelAndView = new ModelAndView("edit-offer");
        modelAndView.addObject("offerId", id);
        modelAndView.addObject("offerRequestDTO", this.offerService.getOfferForEdit(id, getUserId(httpSession)));

        return modelAndView;
    }

    @PostMapping("/offers/edit/{id}")
    public ModelAndView editOffer(@PathVariable final UUID id,
                                  @Valid final OfferRequestDTO offerRequestDTO,
                                  final BindingResult bindingResult,
                                  final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("edit-offer");
            modelAndView.addObject("offerId", id);
            modelAndView.addObject("offerRequestDTO", offerRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.offerRequestDTO", bindingResult);

            return modelAndView;
        }

        this.offerService.updateOffer(id, offerRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/offers/" + id);
    }

    @PostMapping("/offers/delete/{id}")
    public ModelAndView deleteOffer(@PathVariable final UUID id,
                                    final HttpSession httpSession) {

        this.offerService.deleteOffer(id, getUserId(httpSession));

        return new ModelAndView("redirect:/my-offers");
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute("user_id");
    }
}
