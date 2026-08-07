package photomarketplace.web.user;

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
import photomarketplace.exception.user.UserRoleManagementException;
import photomarketplace.model.dto.user.UserRoleUpdateDTO;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.user.AdminUserService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ModelAndView getUsers(final HttpSession httpSession) {
        final UUID administratorId = getUserId(httpSession);
        final ModelAndView modelAndView = new ModelAndView("admin-users");
        modelAndView.addObject("users", this.adminUserService.getUsers(administratorId));
        modelAndView.addObject("roles", List.of(UserRole.values()));
        modelAndView.addObject("administratorId", administratorId);

        return modelAndView;
    }

    @PutMapping("/{targetUserId}/role")
    public ModelAndView updateUserRole(
            @PathVariable final UUID targetUserId,
            @Valid @ModelAttribute final UserRoleUpdateDTO roleUpdateDTO,
            final BindingResult bindingResult,
            final HttpSession httpSession,
            final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().getFirst().getDefaultMessage());

            return redirectToUsers();
        }

        try {
            this.adminUserService.updateUserRole(
                    getUserId(httpSession),
                    targetUserId,
                    roleUpdateDTO
            );

            redirectAttributes.addFlashAttribute("successMessage", "The user role was updated successfully.");
        } catch (UserRoleManagementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return redirectToUsers();
    }

    private static ModelAndView redirectToUsers() {
        return new ModelAndView("redirect:/admin/users");
    }

    private static UUID getUserId(final HttpSession httpSession) {
        return (UUID) httpSession.getAttribute("user_id");
    }
}
