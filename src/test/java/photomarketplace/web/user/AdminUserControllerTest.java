package photomarketplace.web.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import photomarketplace.exception.user.UserRoleManagementException;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRoleUpdateDTO;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.user.AdminUserService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private static final UUID ADMINISTRATOR_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private AdminUserService adminUserService;

    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        this.adminUserController = new AdminUserController(this.adminUserService);
    }

    @Test
    void getUsersShouldReturnUsersRolesAndAdministratorId() {
        final List<UserDTO> users = List.of(UserDTO.builder()
                .id(TARGET_USER_ID)
                .email("client@example.com")
                .role(UserRole.CLIENT)
                .build());

        when(this.adminUserService.getUsers(ADMINISTRATOR_ID)).thenReturn(users);

        final ModelAndView modelAndView = this.adminUserController.getUsers(administratorSession());

        assertEquals("admin-users", modelAndView.getViewName());
        assertSame(users, modelAndView.getModel().get("users"));
        assertEquals(List.of(UserRole.values()), modelAndView.getModel().get("roles"));
        assertEquals(ADMINISTRATOR_ID, modelAndView.getModel().get("administratorId"));
    }

    @Test
    void updateUserRoleShouldDelegateAndRedirectWithConfirmation() {
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder()
                .role(UserRole.PHOTOGRAPHER)
                .build();
        final BindingResult bindingResult = new BeanPropertyBindingResult(update, "userRoleUpdateDTO");
        final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        final ModelAndView modelAndView = this.adminUserController.updateUserRole(
                TARGET_USER_ID,
                update,
                bindingResult,
                administratorSession(),
                redirectAttributes
        );

        assertEquals("redirect:/admin/users", modelAndView.getViewName());
        assertEquals("The user role was updated successfully.",
                redirectAttributes.getFlashAttributes().get("successMessage"));

        verify(this.adminUserService).updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update);
    }

    @Test
    void updateUserRoleShouldRedirectWithValidationMessage() {
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder().build();
        final BindingResult bindingResult = new BeanPropertyBindingResult(update, "userRoleUpdateDTO");
        final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        bindingResult.addError(new FieldError(
                "userRoleUpdateDTO",
                "role",
                "Select a user role"
        ));

        final ModelAndView modelAndView = this.adminUserController.updateUserRole(
                TARGET_USER_ID,
                update,
                bindingResult,
                administratorSession(),
                redirectAttributes
        );

        assertEquals("redirect:/admin/users", modelAndView.getViewName());
        assertEquals("Select a user role", redirectAttributes.getFlashAttributes().get("errorMessage"));

        verify(this.adminUserService, never()).updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update);
    }

    @Test
    void updateUserRoleShouldRedirectWithServiceError() {
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder()
                .role(UserRole.CLIENT)
                .build();
        final BindingResult bindingResult = new BeanPropertyBindingResult(update, "userRoleUpdateDTO");
        final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        final String errorMessage = "The user already has the selected role.";

        doThrow(new UserRoleManagementException(errorMessage))
                .when(this.adminUserService)
                .updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update);

        final ModelAndView modelAndView = this.adminUserController.updateUserRole(
                TARGET_USER_ID,
                update,
                bindingResult,
                administratorSession(),
                redirectAttributes
        );

        assertEquals("redirect:/admin/users", modelAndView.getViewName());
        assertEquals(errorMessage, redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    private static MockHttpSession administratorSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", ADMINISTRATOR_ID);

        return session;
    }
}
