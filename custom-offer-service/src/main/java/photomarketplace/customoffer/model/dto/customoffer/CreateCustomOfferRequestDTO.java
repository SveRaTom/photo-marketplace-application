package photomarketplace.customoffer.model.dto.customoffer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateCustomOfferRequestDTO(
        @NotNull UUID clientId,
        @NotNull UUID photographerId,
        @NotNull UUID offerId,
        @NotNull @Future LocalDate eventDate,
        @NotBlank @Size(max = 255) String location,
        @NotBlank @Size(min = 10, max = 2000) String message) {
}
