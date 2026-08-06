package photomarketplace.customoffer.model.dto.customoffer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CustomOfferDecisionRequestDTO(
        @NotNull CustomOfferDecisionDTO decision,
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal proposedPrice) {
}
