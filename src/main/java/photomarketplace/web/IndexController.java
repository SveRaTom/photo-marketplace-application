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

//    @GetMapping("/home")
//    public ModelAndView home() {
//        return new ModelAndView("home");
//    }

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

            return modelAndView;
        }

        final UserDTO user = this.userService.login(userLoginRequest);
        httpSession.setAttribute("user_id", user.getId());

        return new ModelAndView("redirect:/home");
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

            return modelAndView;
        }

        this.userService.register(userRegisterRequest);

        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(final HttpSession httpSession) {
        final UserDTO user = this.userService.getUserById((UUID) httpSession.getAttribute("user_id"));

        final ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @GetMapping("/logout")
    public ModelAndView getLogoutPage(final HttpSession httpSession) {
        httpSession.invalidate();

        return new ModelAndView("redirect:/");
    }
}
