package photomarketplace.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import photomarketplace.exception.ForbiddenOperationException;
import photomarketplace.exception.InvalidOperationException;
import photomarketplace.exception.ResourceNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleResourceNotFound(
            final ResourceNotFoundException exception,
            final HttpServletRequest request) {

        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                exception.getMessage(),
                exception,
                request
        );
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ModelAndView handleForbiddenOperation(
            final ForbiddenOperationException exception,
            final HttpServletRequest request) {

        return createErrorResponse(
                HttpStatus.FORBIDDEN,
                "Operation not permitted",
                exception.getMessage(),
                exception,
                request
        );
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ModelAndView handleInvalidOperation(
            final InvalidOperationException exception,
            final HttpServletRequest request) {

        return createErrorResponse(
                HttpStatus.CONFLICT,
                "Request could not be completed",
                exception.getMessage(),
                exception,
                request
        );
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ModelAndView handleInvalidRequest(
            final Exception exception,
            final HttpServletRequest request) {

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "The request contains an invalid value.",
                exception,
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleUnknownRoute(
            final NoResourceFoundException exception,
            final HttpServletRequest request) {

        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                "Page not found",
                "The requested page does not exist.",
                exception,
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleUnsupportedMethod(
            final HttpRequestMethodNotSupportedException exception,
            final HttpServletRequest request) {

        return createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "This action is not supported for the requested page.",
                exception,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedException(
            final Exception exception,
            final HttpServletRequest request) {

        LOGGER.error("Unexpected failure while processing {} {}.",
                request.getMethod(), request.getRequestURI(), exception);

        return errorView(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                "We could not complete your request. Please try again."
        );
    }

    private static ModelAndView createErrorResponse(
            final HttpStatus status,
            final String title,
            final String message,
            final Exception exception,
            final HttpServletRequest request) {

        LOGGER.warn("Request {} {} returned {}: {}",
                request.getMethod(), request.getRequestURI(), status.value(), exception.getMessage());

        return errorView(status, title, message);
    }

    private static ModelAndView errorView(
            final HttpStatus status,
            final String title,
            final String message) {

        final ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("errorTitle", title);
        modelAndView.addObject("errorMessage", message);

        return modelAndView;
    }
}
