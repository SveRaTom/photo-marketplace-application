package photomarketplace.customoffer.model.dto.customoffer;

import photomarketplace.customoffer.model.entity.CustomOfferRequest;
import photomarketplace.customoffer.model.entity.CustomOfferStatus;

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
        CustomOfferStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CustomOfferResponseDTO from(final CustomOfferRequest customOffer) {
        return new CustomOfferResponseDTO(
                customOffer.getId(),
                customOffer.getClientId(),
                customOffer.getPhotographerId(),
                customOffer.getOfferId(),
                customOffer.getEventDate(),
                customOffer.getLocation(),
                customOffer.getMessage(),
                customOffer.getProposedPrice(),
                customOffer.getStatus(),
                customOffer.getCreatedAt(),
                customOffer.getUpdatedAt());
    }
}
