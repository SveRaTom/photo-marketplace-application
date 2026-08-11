package photomarketplace.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import photomarketplace.service.customoffer.CustomOfferService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomOfferService customOfferService;

    @Test
    void publicLandingPageIsAccessibleWithoutAuthentication() throws Exception {
        this.mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedPageRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/bookings"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void clientCanAuthenticateAndPhotographerOnlyPageIsForbidden() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(get("/home").session(clientSession))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/my-offers").session(clientSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void photographerCanAccessPhotographerOnlyPage() throws Exception {
        final MockHttpSession photographerSession = login("photographer@example.com", "testPhotographerPassword");

        this.mockMvc.perform(get("/my-offers").session(photographerSession))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsReturnToLoginPage() throws Exception {
        this.mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "client@example.com")
                        .param("password", "incorrectPassword"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(post("/bookings/create/" + UUID.randomUUID())
                        .session(clientSession)
                        .param("eventDate", "2099-01-01")
                        .param("location", "Sofia"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customOfferPageRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/offers/" + UUID.randomUUID() + "/custom-offer"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void photographerCannotAccessClientCustomOfferPage() throws Exception {
        final MockHttpSession photographerSession = login("photographer@example.com", "testPhotographerPassword");

        this.mockMvc.perform(get("/offers/" + UUID.randomUUID() + "/custom-offer")
                        .session(photographerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void customOfferSubmissionWithoutCsrfTokenIsRejected() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(post("/offers/" + UUID.randomUUID() + "/custom-offer")
                        .session(clientSession)
                        .param("eventDate", "2099-01-01")
                        .param("location", "Sofia")
                        .param("message", "Outdoor portrait photography session"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCustomOffersPageRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/custom-offers"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void photographerCannotAccessClientCustomOffersPage() throws Exception {
        final MockHttpSession photographerSession = login("photographer@example.com", "testPhotographerPassword");

        this.mockMvc.perform(get("/custom-offers").session(photographerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void customOfferWithdrawalWithoutCsrfTokenIsRejected() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(delete("/custom-offers/" + UUID.randomUUID())
                        .session(clientSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void photographerCustomOffersPageRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/photographer/custom-offers"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void clientCannotAccessPhotographerCustomOffersPage() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(get("/photographer/custom-offers").session(clientSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void photographerCustomOfferDecisionWithoutCsrfTokenIsRejected() throws Exception {
        final MockHttpSession photographerSession = login("photographer@example.com", "testPhotographerPassword");

        this.mockMvc.perform(put("/photographer/custom-offers/" + UUID.randomUUID())
                        .session(photographerSession)
                        .param("decision", "ACCEPT")
                        .param("proposedPrice", "450.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void clientCannotAccessPhotographerDashboard() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(get("/dashboard").session(clientSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void photographerCanAccessAndRenderDashboard() throws Exception {
        final MockHttpSession photographerSession = login("photographer@example.com", "testPhotographerPassword");

        when(this.customOfferService.getPhotographerCustomOffers(any(UUID.class))).thenReturn(List.of());

        this.mockMvc.perform(get("/dashboard").session(photographerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void authenticationShouldMatchEmailIgnoringCase() throws Exception {
        final MockHttpSession clientSession = login("CLIENT@EXAMPLE.COM", "testClientPassword");

        this.mockMvc.perform(get("/home").session(clientSession))
                .andExpect(status().isOk());
    }

    @Test
    void profileRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/profile"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void clientAndPhotographerCanAccessAndRenderOwnProfiles() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");
        final MockHttpSession photographerSession = login("photographer@example.com", "testPhotographerPassword");

        this.mockMvc.perform(get("/profile").session(clientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));

        this.mockMvc.perform(get("/profile").session(photographerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }

    @Test
    void profileUpdateWithoutCsrfTokenIsRejected() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(put("/profile")
                        .session(clientSession)
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("email", "client@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileValidationErrorsAreRendered() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(put("/profile")
                        .session(clientSession)
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "M")
                        .param("email", "invalid-email")
                        .param("profileImageUrl", "ftp://example.com/avatar.jpg"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeHasFieldErrors(
                        "profileUpdateDTO",
                        "firstName",
                        "lastName",
                        "email",
                        "profileImageUrl"));
    }

    @Test
    void adminUsersPageRedirectsUnauthenticatedUserToLogin() throws Exception {
        this.mockMvc.perform(get("/admin/users"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void clientAndPhotographerCannotAccessAdminUsersPage() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");
        final MockHttpSession photographerSession = login(
                "photographer@example.com",
                "testPhotographerPassword"
        );

        this.mockMvc.perform(get("/admin/users").session(clientSession))
                .andExpect(status().isForbidden());

        this.mockMvc.perform(get("/admin/users").session(photographerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanAccessAndRenderAdminUsersPage() throws Exception {
        final MockHttpSession administratorSession = login(
                "admin@example.com",
                "testAdminPassword"
        );

        this.mockMvc.perform(get("/admin/users").session(administratorSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("users", "roles", "administratorId"));
    }

    @Test
    void administratorRoleUpdateWithoutCsrfTokenIsRejected() throws Exception {
        final MockHttpSession administratorSession = login(
                "admin@example.com",
                "testAdminPassword"
        );

        this.mockMvc.perform(put("/admin/users/" + UUID.randomUUID() + "/role")
                        .session(administratorSession)
                        .param("role", "PHOTOGRAPHER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationCannotCreateAdministratorAccount() throws Exception {
        this.mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "forged-administrator")
                        .param("email", "forged-admin@example.com")
                        .param("password", "forgedPassword")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute(
                        "formError",
                        "Only client or photographer accounts can be created through registration."
                ));
    }

    @Test
    void authenticatedUserCanLogout() throws Exception {
        final MockHttpSession clientSession = login("client@example.com", "testClientPassword");

        this.mockMvc.perform(post("/logout")
                        .session(clientSession)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    private MockHttpSession login(final String email, final String password) throws Exception {
        final MvcResult result = this.mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/home"))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
