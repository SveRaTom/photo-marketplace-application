package photomarketplace.service.photo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.exception.ForbiddenOperationException;
import photomarketplace.exception.ResourceNotFoundException;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.photo.PhotoDTO;
import photomarketplace.model.dto.photo.PhotoRequestDTO;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.photo.Photo;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.repository.photo.PhotoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final UUID PHOTO_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_PHOTO_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OFFER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PHOTOGRAPHER_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private OfferRepository offerRepository;

    private PhotoService photoService;

    @BeforeEach
    void setUp() {
        this.photoService = new PhotoService(this.photoRepository, this.offerRepository);
    }

    @Test
    void getPhotosForPhotographerShouldMapOwnedCoverPhoto() {
        final Photo photo = photo(true);

        when(this.photoRepository.findAllByOfferPhotographerIdOrderByCreatedAtDesc(PHOTOGRAPHER_ID))
                .thenReturn(List.of(photo));

        final List<PhotoDTO> photos = this.photoService.getPhotosForPhotographer(PHOTOGRAPHER_ID);

        assertEquals(1, photos.size());

        final PhotoDTO result = photos.getFirst();
        assertEquals(PHOTO_ID, result.getId());
        assertEquals("Wedding portrait", result.getTitle());
        assertEquals("https://example.com/photo.jpg", result.getImageUrl());
        assertEquals("A wedding portrait", result.getDescription());
        assertEquals(OFFER_ID, result.getOfferId());
        assertEquals(PHOTOGRAPHER_ID, result.getPhotographerId());
        assertEquals("Wedding photography", result.getOffer().getTitle());
        assertTrue(result.isCoverPhoto());
        assertTrue(result.isCanEdit());
        assertTrue(result.isCanDelete());
        assertFalse(result.isCanSetAsCover());
    }

    @Test
    void getPhotosForOfferShouldMapReadOnlyGalleryPhotoForVisitor() {
        final Photo photo = photo(false);

        when(this.photoRepository.findAllByOfferIdOrderByCreatedAtDesc(OFFER_ID))
                .thenReturn(List.of(photo));

        final PhotoDTO result = this.photoService.getPhotosForOffer(OFFER_ID, OTHER_USER_ID)
                .getFirst();

        assertFalse(result.isCoverPhoto());
        assertFalse(result.isCanEdit());
        assertFalse(result.isCanDelete());
        assertFalse(result.isCanSetAsCover());
    }

    @Test
    void getPhotoByIdShouldAllowOwnerToSetNonCoverPhotoAsCover() {
        final Photo photo = photo(false);

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

        final PhotoDTO result = this.photoService.getPhotoById(PHOTO_ID, PHOTOGRAPHER_ID);

        assertTrue(result.isCanEdit());
        assertTrue(result.isCanDelete());
        assertTrue(result.isCanSetAsCover());
    }

    @Test
    void getPhotoForEditShouldMapOwnedPhoto() {
        final Photo photo = photo(true);

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

        final PhotoRequestDTO result = this.photoService.getPhotoForEdit(PHOTO_ID, PHOTOGRAPHER_ID);

        assertEquals(photo.getTitle(), result.getTitle());
        assertEquals(photo.getImageUrl(), result.getImageUrl());
        assertEquals(photo.getDescription(), result.getDescription());
        assertTrue(result.isCoverPhoto());
    }

    @Test
    void getOfferForPhotoCreateShouldMapOwnedOffer() {
        final Offer offer = offer();

        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));

        final OfferDTO result = this.photoService.getOfferForPhotoCreate(OFFER_ID, PHOTOGRAPHER_ID);

        assertEquals(OFFER_ID, result.getId());
        assertEquals("Wedding photography", result.getTitle());
        assertEquals(new BigDecimal("1200.00"), result.getPrice());
        assertEquals(PHOTOGRAPHER_ID, result.getPhotographer().getId());
    }

    @Test
    void createPhotoShouldNormalizeValuesAndMakeFirstPhotoCover() {
        final Offer offer = offer();
        final PhotoRequestDTO request = photoRequest(
                "  Ceremony portrait  ",
                "  https://example.com/ceremony.jpg  ",
                "   ",
                false
        );

        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));
        when(this.photoRepository.save(any(Photo.class))).thenAnswer(invocation -> {
            final Photo savedPhoto = invocation.getArgument(0);
            savedPhoto.setId(PHOTO_ID);

            return savedPhoto;
        });

        final UUID result = this.photoService.createPhoto(OFFER_ID, request, PHOTOGRAPHER_ID);

        assertEquals(PHOTO_ID, result);

        final ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);

        verify(this.photoRepository).save(photoCaptor.capture());

        final Photo savedPhoto = photoCaptor.getValue();
        assertEquals("Ceremony portrait", savedPhoto.getTitle());
        assertEquals("https://example.com/ceremony.jpg", savedPhoto.getImageUrl());
        assertNull(savedPhoto.getDescription());
        assertSame(offer, savedPhoto.getOffer());
        assertSame(savedPhoto, offer.getCoverPhoto());

        verify(this.offerRepository).save(offer);
    }

    @Test
    void createPhotoShouldReplaceExistingCoverWhenRequested() {
        final Offer offer = offer();
        offer.setCoverPhoto(photo(offer, OTHER_PHOTO_ID));

        final PhotoRequestDTO request =
                photoRequest(null, "https://example.com/new.jpg", null, true);

        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));
        when(this.photoRepository.save(any(Photo.class))).thenAnswer(invocation -> {
            final Photo savedPhoto = invocation.getArgument(0);
            savedPhoto.setId(PHOTO_ID);

            return savedPhoto;
        });

        this.photoService.createPhoto(OFFER_ID, request, PHOTOGRAPHER_ID);

        assertEquals(PHOTO_ID, offer.getCoverPhoto().getId());
        assertNull(offer.getCoverPhoto().getTitle());
        assertNull(offer.getCoverPhoto().getDescription());

        verify(this.offerRepository).save(offer);
    }

    @Test
    void createPhotoShouldKeepExistingCoverWhenNewPhotoIsNotCover() {
        final Offer offer = offer();
        final Photo existingCover = photo(offer, OTHER_PHOTO_ID);
        offer.setCoverPhoto(existingCover);

        final PhotoRequestDTO request =
                photoRequest("Gallery", "https://example.com/gallery.jpg", null, false);

        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer));
        when(this.photoRepository.save(any(Photo.class))).thenAnswer(invocation -> {
            final Photo savedPhoto = invocation.getArgument(0);
            savedPhoto.setId(PHOTO_ID);

            return savedPhoto;
        });

        this.photoService.createPhoto(OFFER_ID, request, PHOTOGRAPHER_ID);

        assertSame(existingCover, offer.getCoverPhoto());

        verify(this.offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void updatePhotoShouldUpdateValuesAndSetPhotoAsCover() {
        final Photo photo = photo(false);
        final Offer offer = photo.getOffer();
        final PhotoRequestDTO request = photoRequest(
                "  Updated title ",
                " https://example.com/updated.jpg ",
                " Updated description ",
                true
        );

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(this.photoRepository.save(photo)).thenReturn(photo);

        this.photoService.updatePhoto(PHOTO_ID, request, PHOTOGRAPHER_ID);

        assertEquals("Updated title", photo.getTitle());
        assertEquals("https://example.com/updated.jpg", photo.getImageUrl());
        assertEquals("Updated description", photo.getDescription());
        assertSame(photo, offer.getCoverPhoto());

        verify(this.offerRepository).save(offer);
    }

    @Test
    void updatePhotoShouldRemoveCurrentCoverSelectionWhenUnselected() {
        final Photo photo = photo(true);
        final Offer offer = photo.getOffer();
        final PhotoRequestDTO request =
                photoRequest("Gallery", "https://example.com/gallery.jpg", null, false);

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(this.photoRepository.save(photo)).thenReturn(photo);

        this.photoService.updatePhoto(PHOTO_ID, request, PHOTOGRAPHER_ID);

        assertNull(offer.getCoverPhoto());

        verify(this.offerRepository).save(offer);
    }

    @Test
    void updatePhotoShouldPreserveDifferentCoverPhoto() {
        final Photo photo = photo(false);
        final Offer offer = photo.getOffer();
        final Photo existingCover = photo(offer, OTHER_PHOTO_ID);
        offer.setCoverPhoto(existingCover);

        final PhotoRequestDTO request =
                photoRequest("Gallery", "https://example.com/gallery.jpg", null, false);

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(this.photoRepository.save(photo)).thenReturn(photo);

        this.photoService.updatePhoto(PHOTO_ID, request, PHOTOGRAPHER_ID);

        assertSame(existingCover, offer.getCoverPhoto());

        verify(this.offerRepository).save(offer);
    }

    @Test
    void deletePhotoShouldClearCoverBeforeDeletingIt() {
        final Photo photo = photo(true);
        final Offer offer = photo.getOffer();

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

        this.photoService.deletePhoto(PHOTO_ID, PHOTOGRAPHER_ID);

        assertNull(offer.getCoverPhoto());

        verify(this.offerRepository).saveAndFlush(offer);
        verify(this.photoRepository).delete(photo);
    }

    @Test
    void deletePhotoShouldDeleteNonCoverWithoutUpdatingOffer() {
        final Photo photo = photo(false);

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

        this.photoService.deletePhoto(PHOTO_ID, PHOTOGRAPHER_ID);

        verify(this.offerRepository, never()).saveAndFlush(any(Offer.class));
        verify(this.photoRepository).delete(photo);
    }

    @Test
    void setCoverPhotoShouldPersistOfferWithSelectedPhoto() {
        final Photo photo = photo(false);
        final Offer offer = photo.getOffer();

        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo));

        this.photoService.setCoverPhoto(PHOTO_ID, PHOTOGRAPHER_ID);

        assertSame(photo, offer.getCoverPhoto());

        verify(this.offerRepository).save(offer);
    }

    @Test
    void getPhotoForEditShouldRejectNonOwner() {
        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.of(photo(false)));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.photoService.getPhotoForEdit(PHOTO_ID, OTHER_USER_ID)
        );

        assertEquals("You do not have permission to manage this photo.", exception.getMessage());
    }

    @Test
    void getOfferForPhotoCreateShouldRejectNonOwner() {
        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(offer()));

        final ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> this.photoService.getOfferForPhotoCreate(OFFER_ID, OTHER_USER_ID)
        );

        assertEquals("You do not have permission to manage photos for this offer.",
                exception.getMessage());
    }

    @Test
    void getPhotoByIdShouldRejectMissingPhoto() {
        when(this.photoRepository.findById(PHOTO_ID)).thenReturn(Optional.empty());

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.photoService.getPhotoById(PHOTO_ID, OTHER_USER_ID)
        );

        assertEquals("Photo with id '%s' does not exist.".formatted(PHOTO_ID),
                exception.getMessage());
    }

    @Test
    void getOfferForPhotoCreateShouldRejectMissingOffer() {
        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.empty());

        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.photoService.getOfferForPhotoCreate(OFFER_ID, PHOTOGRAPHER_ID)
        );

        assertEquals("Offer with id '%s' does not exist.".formatted(OFFER_ID),
                exception.getMessage());
        verifyNoInteractions(this.photoRepository);
    }

    private static Photo photo(final boolean coverPhoto) {
        final Offer offer = offer();
        final Photo photo = photo(offer, PHOTO_ID);

        if (coverPhoto) {
            offer.setCoverPhoto(photo);
        }

        return photo;
    }

    private static Photo photo(final Offer offer, final UUID photoId) {
        return Photo.builder()
                .id(photoId)
                .title("Wedding portrait")
                .imageUrl("https://example.com/photo.jpg")
                .description("A wedding portrait")
                .offer(offer)
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 2, 10, 0))
                .build();
    }

    private static Offer offer() {
        return Offer.builder()
                .id(OFFER_ID)
                .title("Wedding photography")
                .description("Complete wedding photography coverage")
                .price(new BigDecimal("1200.00"))
                .durationHours(8)
                .location("Sofia")
                .isAvailable(true)
                .photographer(photographer())
                .build();
    }

    private static User photographer() {
        return User.builder()
                .id(PHOTOGRAPHER_ID)
                .firstName("Alex")
                .lastName("Morgan")
                .username("alexphoto")
                .email("alex@example.com")
                .password("hashedPassword")
                .role(UserRole.PHOTOGRAPHER)
                .isActive(true)
                .build();
    }

    private static PhotoRequestDTO photoRequest(
            final String title,
            final String imageUrl,
            final String description,
            final boolean coverPhoto) {

        return PhotoRequestDTO.builder()
                .title(title)
                .imageUrl(imageUrl)
                .description(description)
                .coverPhoto(coverPhoto)
                .build();
    }
}
