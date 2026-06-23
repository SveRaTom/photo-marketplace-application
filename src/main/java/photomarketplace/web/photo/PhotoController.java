package photomarketplace.web.photo;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.model.dto.photo.PhotoDTO;
import photomarketplace.model.dto.photo.PhotoRequestDTO;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.photo.PhotoService;

import java.util.UUID;

@Controller
public class PhotoController {

    private final PhotoService photoService;
    private final OfferService offerService;

    @Autowired
    public PhotoController(final PhotoService photoService,
                           final OfferService offerService) {

        this.photoService = photoService;
        this.offerService = offerService;
    }

    @GetMapping("/portfolio")
    public ModelAndView getPortfolio(final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("my-photos");
        modelAndView.addObject("photos", this.photoService.getPhotosForPhotographer(getUserId(httpSession)));

        return modelAndView;
    }

    @GetMapping("/photos")
    public ModelAndView getPhotosAlias() {
        return new ModelAndView("redirect:/portfolio");
    }

    @GetMapping("/offers/{offerId}/photos")
    public ModelAndView getOfferPhotos(@PathVariable final UUID offerId, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("offer-photos");
        modelAndView.addObject("offer", this.offerService.getOfferById(offerId));
        modelAndView.addObject("photos", this.photoService.getPhotosForOffer(offerId, getOptionalUserId(httpSession)));

        return modelAndView;
    }

    @GetMapping("/photos/{id}")
    public ModelAndView getPhotoDetails(@PathVariable final UUID id,
                                        @RequestParam(required = false) final String from,
                                        final HttpSession httpSession) {

        final PhotoDTO photo = this.photoService.getPhotoById(id, getOptionalUserId(httpSession));
        final ModelAndView modelAndView = new ModelAndView("photo-details");
        modelAndView.addObject("photo", photo);
        modelAndView.addObject("backUrl", resolveBackUrl(from, photo));

        return modelAndView;
    }

    @GetMapping("/photos/create/{offerId}")
    public ModelAndView getCreatePhotoPage(@PathVariable final UUID offerId, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("create-photo");
        modelAndView.addObject("offerId", offerId);
        modelAndView.addObject("offer", this.photoService.getOfferForPhotoCreate(offerId, getUserId(httpSession)));
        modelAndView.addObject("photoRequestDTO", PhotoRequestDTO.builder().build());

        return modelAndView;
    }

    @PostMapping("/photos/create/{offerId}")
    public ModelAndView createPhoto(@PathVariable final UUID offerId,
                                    @Valid final PhotoRequestDTO photoRequestDTO,
                                    final BindingResult bindingResult,
                                    final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("create-photo");
            modelAndView.addObject("offerId", offerId);
            modelAndView.addObject("offer", this.photoService.getOfferForPhotoCreate(offerId, getUserId(httpSession)));
            modelAndView.addObject("photoRequestDTO", photoRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.photoRequestDTO", bindingResult);

            return modelAndView;
        }

        final UUID photoId = this.photoService.createPhoto(offerId, photoRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/photos/" + photoId + "?from=offer-photos");
    }

    @GetMapping("/photos/edit/{id}")
    public ModelAndView getEditPhotoPage(@PathVariable final UUID id, final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("edit-photo");
        modelAndView.addObject("photoId", id);
        modelAndView.addObject("photo", this.photoService.getPhotoById(id, getUserId(httpSession)));
        modelAndView.addObject("photoRequestDTO", this.photoService.getPhotoForEdit(id, getUserId(httpSession)));

        return modelAndView;
    }

    @PostMapping("/photos/edit/{id}")
    public ModelAndView editPhoto(@PathVariable final UUID id,
                                  @Valid final PhotoRequestDTO photoRequestDTO,
                                  final BindingResult bindingResult,
                                  final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("edit-photo");
            modelAndView.addObject("photoId", id);
            modelAndView.addObject("photo", this.photoService.getPhotoById(id, getUserId(httpSession)));
            modelAndView.addObject("photoRequestDTO", photoRequestDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.photoRequestDTO", bindingResult);

            return modelAndView;
        }

        this.photoService.updatePhoto(id, photoRequestDTO, getUserId(httpSession));

        return new ModelAndView("redirect:/photos/" + id + "?from=offer-photos");
    }

    @PostMapping("/photos/delete/{id}")
    public ModelAndView deletePhoto(@PathVariable final UUID id, final HttpSession httpSession) {
        final PhotoDTO photo = this.photoService.getPhotoById(id, getUserId(httpSession));
        this.photoService.deletePhoto(id, getUserId(httpSession));

        return new ModelAndView("redirect:/offers/" + photo.getOfferId() + "/photos");
    }

    @PostMapping("/photos/{id}/cover")
    public ModelAndView setCoverPhoto(@PathVariable final UUID id, final HttpSession httpSession) {
        this.photoService.setCoverPhoto(id, getUserId(httpSession));

        return new ModelAndView("redirect:/photos/" + id + "?from=offer-photos");
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute("user_id");
    }

    private static UUID getOptionalUserId(final HttpSession httpSession) {
        return httpSession == null ? null : (UUID) httpSession.getAttribute("user_id");
    }

    private static String resolveBackUrl(final String from, final PhotoDTO photo) {
        if ("portfolio".equals(from)) {
            return "/portfolio";
        }

        if ("offer-details".equals(from)) {
            return "/offers/" + photo.getOfferId();
        }

        return "/offers/" + photo.getOfferId() + "/photos";
    }
}
