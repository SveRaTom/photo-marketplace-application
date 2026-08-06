package photomarketplace.customoffer.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import photomarketplace.customoffer.exception.CustomOfferNotFoundException;
import photomarketplace.customoffer.exception.CustomOfferOperationException;
import photomarketplace.customoffer.model.dto.error.ApiErrorResponseDTO;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleValidation(
            final MethodArgumentNotValidException exception,
            final HttpServletRequest request) {

        final Map<String, String> validationErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Invalid value"
                                : fieldError.getDefaultMessage(),
                        (firstMessage, secondMessage) -> firstMessage,
                        LinkedHashMap::new));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request.getRequestURI(),
                validationErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleTypeMismatch(
            final MethodArgumentTypeMismatchException exception,
            final HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '%s'.".formatted(exception.getName()),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(CustomOfferNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleNotFound(
            final CustomOfferNotFoundException exception,
            final HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    @ExceptionHandler(CustomOfferOperationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInvalidOperation(
            final CustomOfferOperationException exception,
            final HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of());
    }

    private static ResponseEntity<ApiErrorResponseDTO> buildResponse(
            final HttpStatus status,
            final String message,
            final String path,
            final Map<String, String> validationErrors) {

        final ApiErrorResponseDTO errorResponse = new ApiErrorResponseDTO(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors);

        return ResponseEntity.status(status).body(errorResponse);
    }
}
