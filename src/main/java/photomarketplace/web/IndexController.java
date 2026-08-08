package photomarketplace.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.exception.user.UserRegistrationException;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserLoginRequestDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.security.MarketplaceSession;
import photomarketplace.service.user.UserService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/")
public class IndexController {

    private static final List<UserRole> PUBLIC_REGISTRATION_ROLES =
            List.of(UserRole.CLIENT, UserRole.PHOTOGRAPHER);

    private final UserService userService;

    @Autowired
    public IndexController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView index() {
        return new ModelAndView("index");
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(required = false) final String error) {
        final UserLoginRequestDTO userLoginRequest = UserLoginRequestDTO.builder().build();

        final ModelAndView modelAndView = new ModelAndView("login");
        modelAndView.addObject("userLoginRequest", userLoginRequest);
        if (error != null) {
            modelAndView.addObject("loginError", "Invalid email or password.");
        }

        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        final UserRegisterRequestDTO userRegisterRequest = UserRegisterRequestDTO.builder().build();

        final ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("userRegisterRequest", userRegisterRequest);
        modelAndView.addObject("registrationRoles", PUBLIC_REGISTRATION_ROLES);

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView register(@Valid final UserRegisterRequestDTO userRegisterRequest,
                                 final BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("register");
            modelAndView.addObject("userRegisterRequest", userRegisterRequest);
            modelAndView.addObject("registrationRoles", PUBLIC_REGISTRATION_ROLES);
            modelAndView.addObject("org.springframework.validation.BindingResult.userRegisterRequest", bindingResult);

            return modelAndView;
        }

        try {
            this.userService.register(userRegisterRequest);
        } catch (UserRegistrationException exception) {
            final ModelAndView modelAndView = new ModelAndView("register");
            modelAndView.addObject("userRegisterRequest", userRegisterRequest);
            modelAndView.addObject("registrationRoles", PUBLIC_REGISTRATION_ROLES);
            modelAndView.addObject("formError", exception.getMessage());

            return modelAndView;
        }

        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(final HttpSession httpSession) {
        final UUID userId = (UUID) httpSession.getAttribute(MarketplaceSession.USER_ID);

        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        final UserDTO user = this.userService.getUserById(userId);
        final ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

}
