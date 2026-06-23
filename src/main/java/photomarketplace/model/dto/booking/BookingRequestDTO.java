package photomarketplace.model.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {

    @NotNull(message = "Event date must not be empty")
    @Future(message = "Event date must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;

    @NotBlank(message = "Event location must not be blank")
    @Size(max = 255, message = "Event location cannot be longer than 255 characters")
    private String location;

    @Size(max = 2000, message = "Additional requirements cannot be longer than 2000 characters")
    private String notes;
}
