package photomarketplace.model.dto.customoffer;

import java.math.BigDecimal;

public record PhotographerCustomOfferViewDTO(
        CustomOfferResponseDTO request,
        String clientDisplayName,
        String offerTitle,
        BigDecimal originalPrice) {
}
