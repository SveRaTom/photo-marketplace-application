package photomarketplace.customoffer.model.dto.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors) {
}
