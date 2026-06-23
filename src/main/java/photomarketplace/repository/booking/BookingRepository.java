package photomarketplace.repository.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import photomarketplace.model.entity.booking.Booking;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findAllByClientIdOrderByEventDateAsc(UUID clientId);

    List<Booking> findAllByOfferPhotographerIdOrderByEventDateAsc(UUID photographerId);
}
