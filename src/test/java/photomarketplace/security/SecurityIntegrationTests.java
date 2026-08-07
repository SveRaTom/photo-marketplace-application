package photomarketplace.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

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
