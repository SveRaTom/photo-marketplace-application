package photomarketplace.web.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import photomarketplace.exception.user.ProfileUpdateException;
import photomarketplace.model.dto.user.ProfileUpdateDTO;
import photomarketplace.security.MarketplaceSession;
import photomarketplace.service.user.UserService;

import java.util.UUID;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ModelAndView getProfile(final HttpSession httpSession) {
        final UUID userId = getUserId(httpSession);
        return profileView(userId, this.userService.getProfileForEdit(userId));
    }

    @PutMapping
    public ModelAndView updateProfile(
            @Valid @ModelAttribute final ProfileUpdateDTO profileUpdateDTO,
            final BindingResult bindingResult,
            final HttpSession httpSession,
            final RedirectAttributes redirectAttributes) {

        final UUID userId = getUserId(httpSession);

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = profileView(userId, profileUpdateDTO);
            modelAndView.addObject("org.springframework.validation.BindingResult.profileUpdateDTO",
                    bindingResult);

            return modelAndView;
        }

        try {
            this.userService.updateProfile(userId, profileUpdateDTO);
        } catch (ProfileUpdateException exception) {
            final ModelAndView modelAndView = profileView(userId, profileUpdateDTO);
            modelAndView.addObject("formError", exception.getMessage());

            return modelAndView;
        }

        redirectAttributes.addFlashAttribute("successMessage", "Your profile was updated successfully.");

        return new ModelAndView("redirect:/profile");
    }

    private ModelAndView profileView(final UUID userId, final ProfileUpdateDTO profileUpdateDTO) {
        final ModelAndView modelAndView = new ModelAndView("profile");
        modelAndView.addObject("user", this.userService.getUserById(userId));
        modelAndView.addObject("profileUpdateDTO", profileUpdateDTO);

        return modelAndView;
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute(MarketplaceSession.USER_ID);
    }
}
