package photomarketplace.web.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import photomarketplace.exception.ForbiddenOperationException;
import photomarketplace.exception.InvalidOperationException;
import photomarketplace.exception.ResourceNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        this.exceptionHandler = new GlobalExceptionHandler();
        this.request = new MockHttpServletRequest("GET", "/requested-resource");
    }

    @Test
    void resourceNotFoundShouldReturn404WithExceptionMessage() {
        final ModelAndView response = this.exceptionHandler.handleResourceNotFound(
                new ResourceNotFoundException("The offer does not exist."),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "The offer does not exist."
        );
    }

    @Test
    void forbiddenOperationShouldReturn403WithExceptionMessage() {
        final ModelAndView response = this.exceptionHandler.handleForbiddenOperation(
                new ForbiddenOperationException("You cannot manage this offer."),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "Operation not permitted",
                "You cannot manage this offer."
        );
    }

    @Test
    void invalidOperationShouldReturn409WithExceptionMessage() {
        final ModelAndView response = this.exceptionHandler.handleInvalidOperation(
                new InvalidOperationException("The booking is no longer pending."),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "Request could not be completed",
                "The booking is no longer pending."
        );
    }

    @Test
    void invalidBuiltInRequestShouldReturnSafe400Message() {
        final ModelAndView response = this.exceptionHandler.handleInvalidRequest(
                new IllegalArgumentException("Internal conversion detail"),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "The request contains an invalid value."
        );
    }

    @Test
    void unknownRouteShouldReturn404() {
        final ModelAndView response = this.exceptionHandler.handleUnknownRoute(
                new NoResourceFoundException(HttpMethod.GET, "missing-page"),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "Page not found",
                "The requested page does not exist."
        );
    }

    @Test
    void unsupportedMethodShouldReturn405() {
        final ModelAndView response = this.exceptionHandler.handleUnsupportedMethod(
                new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "This action is not supported for the requested page."
        );
    }

    @Test
    void unexpectedExceptionShouldReturnSafe500Message() {
        final ModelAndView response = this.exceptionHandler.handleUnexpectedException(
                new IllegalStateException("Sensitive internal detail"),
                this.request
        );

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                "We could not complete your request. Please try again."
        );
    }

    private static void assertErrorResponse(
            final ModelAndView response,
            final HttpStatus expectedStatus,
            final String expectedTitle,
            final String expectedMessage) {

        assertEquals("error", response.getViewName());
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedStatus.value(), response.getModel().get("status"));
        assertEquals(expectedTitle, response.getModel().get("errorTitle"));
        assertEquals(expectedMessage, response.getModel().get("errorMessage"));
    }
}
