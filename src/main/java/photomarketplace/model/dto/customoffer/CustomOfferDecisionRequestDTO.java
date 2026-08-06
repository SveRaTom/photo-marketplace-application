package photomarketplace.model.dto.customoffer;

import java.math.BigDecimal;

public record CustomOfferDecisionRequestDTO(
        CustomOfferDecisionDTO decision,
        BigDecimal proposedPrice) {
}
