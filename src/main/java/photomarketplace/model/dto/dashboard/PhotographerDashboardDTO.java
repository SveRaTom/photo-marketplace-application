package photomarketplace.model.dto.dashboard;

import lombok.Builder;
import lombok.Getter;
import photomarketplace.model.dto.booking.BookingDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.dto.user.UserDTO;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
public class PhotographerDashboardDTO {

    private UserDTO photographer;
    private long totalOffers;
    private long availableOffers;
    private long totalBookings;
    private long pendingRequests;
    private BigDecimal averageRating;
    private List<BookingDTO> upcomingBookings;
    private List<ReviewDTO> recentReviews;
}
