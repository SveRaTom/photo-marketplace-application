package photomarketplace.service.photo;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.photo.PhotoDTO;
import photomarketplace.model.dto.photo.PhotoRequestDTO;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.photo.Photo;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.repository.photo.PhotoRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final OfferRepository offerRepository;

    @Autowired
    public PhotoService(final PhotoRepository photoRepository,
                        final OfferRepository offerRepository) {

        this.photoRepository = photoRepository;
        this.offerRepository = offerRepository;
    }

    public List<PhotoDTO> getPhotosForPhotographer(final UUID photographerId) {
        return this.photoRepository.findAllByOfferPhotographerIdOrderByCreatedAtDesc(photographerId).stream()
                .map(photo -> toPhotoDTO(photo, photographerId))
                .toList();
    }

    public List<PhotoDTO> getPhotosForOffer(final UUID offerId, final UUID currentUserId) {
        return this.photoRepository.findAllByOfferIdOrderByCreatedAtDesc(offerId).stream()
                .map(photo -> toPhotoDTO(photo, currentUserId))
                .toList();
    }

    public PhotoDTO getPhotoById(final UUID photoId, final UUID currentUserId) {
        return toPhotoDTO(getPhoto(photoId), currentUserId);
    }

    public PhotoRequestDTO getPhotoForEdit(final UUID photoId, final UUID photographerId) {
        final Photo photo = getOwnedPhoto(photoId, photographerId);

        return PhotoRequestDTO.builder()
                .title(photo.getTitle())
                .imageUrl(photo.getImageUrl())
                .description(photo.getDescription())
                .coverPhoto(isCoverPhoto(photo))
                .build();
    }

    public OfferDTO getOfferForPhotoCreate(final UUID offerId, final UUID photographerId) {
        return toOfferSummaryDTO(getOwnedOffer(offerId, photographerId));
    }

    public UUID createPhoto(final UUID offerId,
                            final PhotoRequestDTO photoRequest,
                            final UUID photographerId) {

        final Offer offer = getOwnedOffer(offerId, photographerId);

        final Photo photo = Photo.builder()
                .title(normalize(photoRequest.getTitle()))
                .imageUrl(photoRequest.getImageUrl().trim())
                .description(normalize(photoRequest.getDescription()))
                .offer(offer)
                .build();

        final Photo savedPhoto = this.photoRepository.save(photo);

        if (photoRequest.isCoverPhoto() || offer.getCoverPhoto() == null) {
            offer.setCoverPhoto(savedPhoto);
            this.offerRepository.save(offer);
        }

        return savedPhoto.getId();
    }

    public void updatePhoto(final UUID photoId,
                            final PhotoRequestDTO photoRequest,
                            final UUID photographerId) {

        final Photo photo = getOwnedPhoto(photoId, photographerId);
        final Offer offer = photo.getOffer();

        photo.setTitle(normalize(photoRequest.getTitle()));
        photo.setImageUrl(photoRequest.getImageUrl().trim());
        photo.setDescription(normalize(photoRequest.getDescription()));

        final Photo savedPhoto = this.photoRepository.save(photo);

        if (photoRequest.isCoverPhoto()) {
            offer.setCoverPhoto(savedPhoto);
        } else if (isCoverPhoto(savedPhoto)) {
            offer.setCoverPhoto(null);
        }

        this.offerRepository.save(offer);
    }

    public void deletePhoto(final UUID photoId, final UUID photographerId) {
        final Photo photo = getOwnedPhoto(photoId, photographerId);
        final Offer offer = photo.getOffer();

        if (isCoverPhoto(photo)) {
            offer.setCoverPhoto(null);
            this.offerRepository.saveAndFlush(offer);
        }

        this.photoRepository.delete(photo);
    }

    public void setCoverPhoto(final UUID photoId, final UUID photographerId) {
        final Photo photo = getOwnedPhoto(photoId, photographerId);
        final Offer offer = photo.getOffer();

        offer.setCoverPhoto(photo);
        this.offerRepository.save(offer);
    }

    private Photo getOwnedPhoto(final UUID photoId, final UUID photographerId) {
        final Photo photo = getPhoto(photoId);

        if (!photo.getOffer().getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("You do not have permission to manage this photo.");
        }

        return photo;
    }

    private Offer getOwnedOffer(final UUID offerId, final UUID photographerId) {
        final Offer offer = getOffer(offerId);

        if (!offer.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("You do not have permission to manage photos for this offer.");
        }

        return offer;
    }

    private Photo getPhoto(final UUID photoId) {
        return this.photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo with id '%s' does not exist.".formatted(photoId)));
    }

    private Offer getOffer(final UUID offerId) {
        return this.offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer with id '%s' does not exist.".formatted(offerId)));
    }

    private PhotoDTO toPhotoDTO(final Photo photo, final UUID currentUserId) {
        final Offer offer = photo.getOffer();
        final boolean owner = offer.getPhotographer().getId().equals(currentUserId);
        final boolean coverPhoto = isCoverPhoto(photo);

        return PhotoDTO.builder()
                .id(photo.getId())
                .title(photo.getTitle())
                .imageUrl(photo.getImageUrl())
                .description(photo.getDescription())
                .offerId(offer.getId())
                .photographerId(offer.getPhotographer().getId())
                .offer(toOfferSummaryDTO(offer))
                .coverPhoto(coverPhoto)
                .canEdit(owner)
                .canDelete(owner)
                .canSetAsCover(owner && !coverPhoto)
                .createdAt(photo.getCreatedAt())
                .updatedAt(photo.getUpdatedAt())
                .build();
    }

    private OfferDTO toOfferSummaryDTO(final Offer offer) {
        return OfferDTO.builder()
                .id(offer.getId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .price(offer.getPrice())
                .durationHours(offer.getDurationHours())
                .location(offer.getLocation())
                .isAvailable(offer.isAvailable())
                .photographer(UserMapper.toUserDTO(offer.getPhotographer()))
                .build();
    }

    private static boolean isCoverPhoto(final Photo photo) {
        return photo.getOffer().getCoverPhoto() != null
                && photo.getOffer().getCoverPhoto().getId().equals(photo.getId());
    }

    private static String normalize(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
