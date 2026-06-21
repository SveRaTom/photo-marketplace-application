package photomarketplace.model.dto.booking;

import lombok.Builder;
import lombok.Data;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.booking.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class BookingDTO {

    private UUID id;
    private LocalDate eventDate;
    private String location;
    private String notes;
    private BookingStatus status;
    private UserDTO client;
    private OfferDTO offer;
    private ReviewDTO review;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
