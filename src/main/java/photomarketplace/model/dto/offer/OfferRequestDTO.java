package photomarketplace.model.dto.offer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferRequestDTO {

    @NotBlank(message = "Title must not be blank")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Description must not be blank")
    @Size(min = 20, max = 3000, message = "Description must be between 20 and 3000 characters")
    private String description;

    @NotNull(message = "Price must not be empty")
    @DecimalMin(value = "0.00", message = "Price must be zero or positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have up to 8 digits and 2 decimals")
    private BigDecimal price;

    @NotNull(message = "Duration must not be empty")
    @Min(value = 1, message = "Duration must be at least 1 hour")
    @Max(value = 24, message = "Duration cannot be more than 24 hours")
    private Integer durationHours;

    @NotBlank(message = "Location must not be blank")
    @Size(max = 255, message = "Location cannot be longer than 255 characters")
    private String location;

    @NotBlank(message = "Cover photo URL must not be blank")
    @Size(max = 500, message = "Cover photo URL cannot be longer than 500 characters")
    private String coverPhotoImageUrl;

    @Builder.Default
    private boolean available = true;
}
