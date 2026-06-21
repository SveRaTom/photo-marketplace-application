package photomarketplace.model.dto.offer;

import lombok.Builder;
import lombok.Data;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.photo.PhotoDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.user.UserDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class OfferDTO {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer durationHours;
    private String location;
    private boolean isAvailable;
    private UserDTO photographer;
    private PhotoDTO coverPhoto;
    private List<PhotoDTO> photos;
    private List<BookingDTO> bookings;
    private List<ReviewDTO> reviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
