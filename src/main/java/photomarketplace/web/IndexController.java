package photomarketplace.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserLoginRequestDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.service.user.UserService;

import java.util.UUID;

@Controller
@RequestMapping("/")
public class IndexController {

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
    public ModelAndView getLoginPage() {
        final UserLoginRequestDTO userLoginRequest = UserLoginRequestDTO.builder().build();

        final ModelAndView modelAndView = new ModelAndView("login");
        modelAndView.addObject("userLoginRequest", userLoginRequest);

        return modelAndView;
    }

    @PostMapping("/login")
    public ModelAndView login(@Valid final UserLoginRequestDTO userLoginRequest,
                              final BindingResult bindingResult,
                              final HttpSession httpSession) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("login");
            modelAndView.addObject("userLoginRequest", userLoginRequest);
            // Ensure the BindingResult is available in the model under the expected key
            modelAndView.addObject("org.springframework.validation.BindingResult.userLoginRequest", bindingResult);

            return modelAndView;
        }

        try {
            final UserDTO user = this.userService.login(userLoginRequest);
            httpSession.setAttribute("user_id", user.getId());
            httpSession.setAttribute("user_role", user.getRole().name());

            return new ModelAndView("redirect:/home");
        } catch (RuntimeException ex) {
            // Authentication failed - return to login page with a user-friendly error message
            final ModelAndView modelAndView = new ModelAndView("login");
            modelAndView.addObject("userLoginRequest", userLoginRequest);
            modelAndView.addObject("org.springframework.validation.BindingResult.userLoginRequest", bindingResult);
            modelAndView.addObject("loginError", ex.getMessage() == null ? "Invalid credentials" : ex.getMessage());

            return modelAndView;
        }
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        final UserRegisterRequestDTO userRegisterRequest = UserRegisterRequestDTO.builder().build();

        final ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("userRegisterRequest", userRegisterRequest);

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView register(@Valid final UserRegisterRequestDTO userRegisterRequest,
                                 final BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            final ModelAndView modelAndView = new ModelAndView("register");
            modelAndView.addObject("userRegisterRequest", userRegisterRequest);
            // Ensure the BindingResult is available in the model under the expected key
            modelAndView.addObject("org.springframework.validation.BindingResult.userRegisterRequest", bindingResult);

            return modelAndView;
        }

        this.userService.register(userRegisterRequest);

        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(final HttpSession httpSession) {
        final UUID userId = (UUID) httpSession.getAttribute("user_id");

        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }
        
        final UserDTO user = this.userService.getUserById(userId);
        final ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @PostMapping("/logout")
    public ModelAndView logout(final HttpSession httpSession) {
        httpSession.invalidate();

        return new ModelAndView("redirect:/");
    }
}
