package photomarketplace.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import photomarketplace.exception.ForbiddenOperationException;
import photomarketplace.exception.InvalidOperationException;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "client@example.com", roles = "CLIENT")
@Import(ErrorHandlingIntegrationTests.ErrorTestController.class)
class ErrorHandlingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedIdentifierShouldRenderBranded400Page() throws Exception {
        this.mockMvc.perform(get("/offers/not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 400))
                .andExpect(content().string(containsString("Invalid request")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    void missingOfferShouldRenderBranded404Page() throws Exception {
        this.mockMvc.perform(get("/offers/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(content().string(containsString("Resource not found")))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Test
    void forbiddenDomainOperationShouldRenderBranded403Page() throws Exception {
        this.mockMvc.perform(get("/test-errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 403))
                .andExpect(content().string(containsString("Operation not permitted")));
    }

    @Test
    void invalidDomainOperationShouldRenderBranded409Page() throws Exception {
        this.mockMvc.perform(get("/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 409))
                .andExpect(content().string(containsString("Request could not be completed")));
    }

    @Test
    void unknownRouteShouldRenderBranded404Page() throws Exception {
        this.mockMvc.perform(get("/page-that-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    void unsupportedMethodShouldRenderBranded405Page() throws Exception {
        this.mockMvc.perform(post("/offers").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 405))
                .andExpect(content().string(containsString("Method not allowed")));
    }

    @Test
    void unexpectedFailureShouldRenderSafe500Page() throws Exception {
        this.mockMvc.perform(get("/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 500))
                .andExpect(content().string(containsString("Something went wrong")))
                .andExpect(content().string(not(containsString("Sensitive internal detail"))))
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }

    @Controller
    @RequestMapping("/test-errors")
    static class ErrorTestController {

        @GetMapping("/forbidden")
        String forbiddenOperation() {
            throw new ForbiddenOperationException("This operation belongs to another user.");
        }

        @GetMapping("/conflict")
        String invalidOperation() {
            throw new InvalidOperationException("This operation conflicts with the current state.");
        }

        @GetMapping("/unexpected")
        String unexpectedFailure() {
            throw new IllegalStateException("Sensitive internal detail");
        }
    }
}
