package photomarketplace.service.offer;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.offer.OfferRequestDTO;
import photomarketplace.model.dto.photo.PhotoDTO;
import photomarketplace.model.dto.review.ReviewDTO;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.photo.Photo;
import photomarketplace.model.entity.review.Review;
import photomarketplace.model.entity.user.User;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.repository.photo.PhotoRepository;
import photomarketplace.service.user.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;
    private final PhotoRepository photoRepository;
    private final UserService userService;

    @Autowired
    public OfferService(final OfferRepository offerRepository,
                        final PhotoRepository photoRepository,
                        final UserService userService) {

        this.offerRepository = offerRepository;
        this.photoRepository = photoRepository;
        this.userService = userService;
    }

    public List<OfferDTO> getAllOffers() {
        return this.offerRepository.findAll().stream()
                .map(this::toOfferDTO)
                .toList();
    }

    public List<OfferDTO> getOffersByPhotographer(final UUID photographerId) {
        return this.offerRepository.findAllByPhotographerId(photographerId).stream()
                .map(this::toOfferDTO)
                .toList();
    }

    public OfferDTO getOfferById(final UUID id) {
        return toOfferDTO(getOffer(id));
    }

    public OfferRequestDTO getOfferForEdit(final UUID id, final UUID photographerId) {
        final Offer offer = getOwnedOffer(id, photographerId);
        final Photo coverPhoto = getCoverPhoto(offer);

        return OfferRequestDTO.builder()
                .title(offer.getTitle())
                .description(offer.getDescription())
                .price(offer.getPrice())
                .durationHours(offer.getDurationHours())
                .location(offer.getLocation())
                .available(offer.isAvailable())
                .coverPhotoImageUrl(coverPhoto == null ? null : coverPhoto.getImageUrl())
                .build();
    }

    public UUID createOffer(final OfferRequestDTO offerRequest, final UUID photographerId) {
        final User photographer = this.userService.getUser(photographerId);

        final Offer offer = Offer.builder()
                .title(offerRequest.getTitle())
                .description(offerRequest.getDescription())
                .price(offerRequest.getPrice())
                .durationHours(offerRequest.getDurationHours())
                .location(offerRequest.getLocation())
                .isAvailable(offerRequest.isAvailable())
                .photographer(photographer)
                .photos(new ArrayList<>())
                .build();

        final Offer savedOffer = this.offerRepository.save(offer);
        upsertCoverPhoto(savedOffer, offerRequest);

        return savedOffer.getId();
    }

    public void updateOffer(final UUID id, final OfferRequestDTO offerRequest, final UUID photographerId) {
        final Offer offer = getOwnedOffer(id, photographerId);

        offer.setTitle(offerRequest.getTitle());
        offer.setDescription(offerRequest.getDescription());
        offer.setPrice(offerRequest.getPrice());
        offer.setDurationHours(offerRequest.getDurationHours());
        offer.setLocation(offerRequest.getLocation());
        offer.setAvailable(offerRequest.isAvailable());

        upsertCoverPhoto(offer, offerRequest);
        this.offerRepository.save(offer);
    }

    public void deleteOffer(final UUID id, final UUID photographerId) {
        final Offer offer = getOwnedOffer(id, photographerId);

        offer.setCoverPhoto(null);
        this.offerRepository.saveAndFlush(offer);
        this.offerRepository.delete(offer);
    }

    private void upsertCoverPhoto(final Offer offer, final OfferRequestDTO offerRequest) {
        final String imageUrl = offerRequest.getCoverPhotoImageUrl();

        if (imageUrl == null || imageUrl.isBlank()) {
            offer.setCoverPhoto(null);
            return;
        }

        Photo coverPhoto = offer.getCoverPhoto();

        if (coverPhoto == null) {
            coverPhoto = Photo.builder()
                    .title(offer.getTitle())
                    .description("Cover photo for " + offer.getTitle())
                    .offer(offer)
                    .build();

            offer.getPhotos().add(coverPhoto);
        }

        coverPhoto.setTitle(offer.getTitle());
        coverPhoto.setImageUrl(imageUrl.trim());
        coverPhoto.setOffer(offer);

        final Photo savedCoverPhoto = this.photoRepository.save(coverPhoto);
        offer.setCoverPhoto(savedCoverPhoto);
        this.offerRepository.saveAndFlush(offer);
    }

    private Offer getOwnedOffer(final UUID offerId, final UUID photographerId) {
        final Offer offer = getOffer(offerId);

        if (!offer.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("You do not have permission to manage this offer.");
        }

        return offer;
    }

    private Offer getOffer(final UUID id) {
        return this.offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer with id [%s] does not exist.".formatted(id)));
    }

    private OfferDTO toOfferDTO(final Offer offer) {
        if (offer == null) {
            return null;
        }

        return OfferDTO.builder()
                .id(offer.getId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .price(offer.getPrice())
                .durationHours(offer.getDurationHours())
                .location(offer.getLocation())
                .isAvailable(offer.isAvailable())
                .photographer(UserMapper.toUserDTO(offer.getPhotographer()))
                .coverPhoto(toPhotoDTO(getCoverPhoto(offer)))
                .photos(offer.getPhotos() == null ? List.of() : offer.getPhotos().stream()
                        .map(this::toPhotoDTO)
                        .toList())
                .bookings(List.of())
                .reviews(offer.getReviews() == null ? List.of() : offer.getReviews().stream()
                        .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                        .map(this::toReviewSummaryDTO)
                        .toList())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }

    private PhotoDTO toPhotoDTO(final Photo photo) {
        if (photo == null) {
            return null;
        }

        return PhotoDTO.builder()
                .id(photo.getId())
                .title(photo.getTitle())
                .imageUrl(photo.getImageUrl())
                .description(photo.getDescription())
                .offerId(photo.getOffer().getId())
                .photographerId(photo.getOffer().getPhotographer().getId())
                .coverPhoto(isCoverPhoto(photo))
                .createdAt(photo.getCreatedAt())
                .updatedAt(photo.getUpdatedAt())
                .build();
    }

    private ReviewDTO toReviewSummaryDTO(final Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .authorId(review.getAuthor().getId())
                .offerId(review.getOffer().getId())
                .bookingId(review.getBooking().getId())
                .author(UserMapper.toUserDTO(review.getAuthor()))
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private static Photo getCoverPhoto(final Offer offer) {
        if (offer.getCoverPhoto() != null) {
            return offer.getCoverPhoto();
        }

        if (offer.getPhotos() == null || offer.getPhotos().isEmpty()) {
            return null;
        }

        return offer.getPhotos().getFirst();
    }

    private static boolean isCoverPhoto(final Photo photo) {
        return photo.getOffer().getCoverPhoto() != null
                && photo.getOffer().getCoverPhoto().getId().equals(photo.getId());
    }
}
