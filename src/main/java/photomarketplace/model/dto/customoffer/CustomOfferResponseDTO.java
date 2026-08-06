package photomarketplace.model.dto.customoffer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomOfferResponseDTO(
        UUID id,
        UUID clientId,
        UUID photographerId,
        UUID offerId,
        LocalDate eventDate,
        String location,
        String message,
        BigDecimal proposedPrice,
        CustomOfferStatusDTO status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
