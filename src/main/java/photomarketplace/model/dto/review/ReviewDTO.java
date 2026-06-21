package photomarketplace.model.dto.review;

import lombok.Builder;
import lombok.Data;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.user.UserDTO;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class ReviewDTO {

    private UUID id;
    private Integer rating;
    private String comment;
    private UserDTO author;
    private OfferDTO offer;
    private BookingDTO booking;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
