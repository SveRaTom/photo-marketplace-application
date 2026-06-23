package photomarketplace.model.dto.photo;

import lombok.Builder;
import lombok.Data;
import photomarketplace.model.dto.offer.OfferDTO;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class PhotoDTO {

    private UUID id;
    private String title;
    private String imageUrl;
    private String description;
    private OfferDTO offer;
    private UUID offerId;
    private UUID photographerId;
    private boolean coverPhoto;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canSetAsCover;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
