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
import photomarketplace.exception.user.ProfileUpdateException;
import photomarketplace.model.dto.user.ProfileUpdateDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.user.UserService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UserService userService;

    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        this.profileController = new ProfileController(this.userService);
    }

    @Test
    void getProfileShouldReturnUserAndEditableProfile() {
        final UserDTO user = user();
        final ProfileUpdateDTO profile = profile();

        when(this.userService.getProfileForEdit(USER_ID)).thenReturn(profile);
        when(this.userService.getUserById(USER_ID)).thenReturn(user);

        final ModelAndView modelAndView = this.profileController.getProfile(userSession());

        assertEquals("profile", modelAndView.getViewName());
        assertSame(user, modelAndView.getModel().get("user"));
        assertSame(profile, modelAndView.getModel().get("profileUpdateDTO"));
    }

    @Test
    void updateProfileShouldDelegateAndRedirectWithConfirmation() {
        final ProfileUpdateDTO profile = profile();
        final BindingResult bindingResult = new BeanPropertyBindingResult(profile, "profileUpdateDTO");
        final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        final ModelAndView modelAndView = this.profileController.updateProfile(
                profile,
                bindingResult,
                userSession(),
                redirectAttributes
        );

        assertEquals("redirect:/profile", modelAndView.getViewName());
        assertEquals("Your profile was updated successfully.",
                redirectAttributes.getFlashAttributes().get("successMessage"));

        verify(this.userService).updateProfile(USER_ID, profile);
    }

    @Test
    void updateProfileShouldReturnProfileWithValidationErrors() {
        final UserDTO user = user();
        final ProfileUpdateDTO profile = profile();
        final BindingResult bindingResult = new BeanPropertyBindingResult(profile, "profileUpdateDTO");

        bindingResult.addError(new FieldError(
                "profileUpdateDTO",
                "email",
                "Enter a valid email address")
        );

        when(this.userService.getUserById(USER_ID)).thenReturn(user);

        final ModelAndView modelAndView = this.profileController.updateProfile(
                profile,
                bindingResult,
                userSession(),
                new RedirectAttributesModelMap()
        );

        assertEquals("profile", modelAndView.getViewName());
        assertSame(user, modelAndView.getModel().get("user"));
        assertSame(profile, modelAndView.getModel().get("profileUpdateDTO"));

        verify(this.userService, never()).updateProfile(USER_ID, profile);
    }

    @Test
    void updateProfileShouldShowServiceError() {
        final UserDTO user = user();
        final ProfileUpdateDTO profile = profile();
        final BindingResult bindingResult = new BeanPropertyBindingResult(profile, "profileUpdateDTO");
        final String errorMessage = "An account with this email already exists.";

        doThrow(new ProfileUpdateException(errorMessage))
                .when(this.userService)
                .updateProfile(USER_ID, profile);
        when(this.userService.getUserById(USER_ID)).thenReturn(user);

        final ModelAndView modelAndView = this.profileController.updateProfile(
                profile,
                bindingResult,
                userSession(),
                new RedirectAttributesModelMap()
        );

        assertEquals("profile", modelAndView.getViewName());
        assertEquals(errorMessage, modelAndView.getModel().get("formError"));
        assertSame(user, modelAndView.getModel().get("user"));
    }

    private static MockHttpSession userSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", USER_ID);

        return session;
    }

    private static UserDTO user() {
        return UserDTO.builder()
                .id(USER_ID)
                .username("client")
                .email("client@example.com")
                .displayName("Alex Morgan")
                .role(UserRole.CLIENT)
                .build();
    }

    private static ProfileUpdateDTO profile() {
        return ProfileUpdateDTO.builder()
                .firstName("Alex")
                .lastName("Morgan")
                .email("client@example.com")
                .build();
    }
}
