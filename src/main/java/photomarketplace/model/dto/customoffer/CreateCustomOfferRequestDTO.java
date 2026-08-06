package photomarketplace.model.dto.customoffer;

import java.time.LocalDate;
import java.util.UUID;

public record CreateCustomOfferRequestDTO(
        UUID clientId,
        UUID photographerId,
        UUID offerId,
        LocalDate eventDate,
        String location,
        String message) {
}
