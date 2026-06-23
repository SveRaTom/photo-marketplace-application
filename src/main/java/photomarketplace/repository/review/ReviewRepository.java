package photomarketplace.repository.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import photomarketplace.model.entity.review.Review;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    List<Review> findAllByOfferPhotographerIdOrderByCreatedAtDesc(UUID photographerId);

    List<Review> findAllByOfferIdOrderByCreatedAtDesc(UUID offerId);

    boolean existsByBookingId(UUID bookingId);
}
