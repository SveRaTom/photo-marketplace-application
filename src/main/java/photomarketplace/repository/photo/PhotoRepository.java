package photomarketplace.repository.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import photomarketplace.model.entity.photo.Photo;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findAllByOfferPhotographerIdOrderByCreatedAtDesc(UUID photographerId);

    List<Photo> findAllByOfferIdOrderByCreatedAtDesc(UUID offerId);
}
