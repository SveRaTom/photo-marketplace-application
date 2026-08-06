package photomarketplace.customoffer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import photomarketplace.customoffer.model.entity.CustomOfferRequest;
import photomarketplace.customoffer.model.entity.CustomOfferStatus;

import java.util.List;
import java.util.UUID;

public interface CustomOfferRequestRepository extends JpaRepository<CustomOfferRequest, UUID> {

    boolean existsByClientIdAndOfferIdAndStatus(UUID clientId, UUID offerId, CustomOfferStatus status);

    List<CustomOfferRequest> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);

    List<CustomOfferRequest> findAllByPhotographerIdOrderByCreatedAtDesc(UUID photographerId);
}
