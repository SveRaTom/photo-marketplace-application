package photomarketplace.model.dto.customoffer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CustomOfferDecisionRequestDTO(
        @NotNull(message = "A decision is required") CustomOfferDecisionDTO decision,
        @DecimalMin(value = "0.01", message = "The proposed price must be positive")
        @Digits(integer = 8, fraction = 2, message = "The proposed price must have up to 8 digits and 2 decimals")
        BigDecimal proposedPrice) {
}
