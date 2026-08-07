package photomarketplace.web.dashboard;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.service.dashboard.DashboardService;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ModelAndView getDashboard(final HttpSession httpSession) {
        final ModelAndView modelAndView = new ModelAndView("dashboard");
        final UUID photographerId = (UUID) httpSession.getAttribute("user_id");

        modelAndView.addObject("dashboard",
                this.dashboardService.getPhotographerDashboard(photographerId));

        return modelAndView;
    }
}
