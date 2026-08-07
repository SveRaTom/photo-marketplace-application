package photomarketplace.config.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import photomarketplace.exception.user.ProfileUpdateException;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.offer.OfferRequestDTO;
import photomarketplace.model.dto.user.ProfileUpdateDTO;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.model.entity.photo.Photo;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.repository.photo.PhotoRepository;
import photomarketplace.repository.user.UserRepository;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.user.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CacheConfiguration.class,
        OfferCacheIntegrationTest.TestConfiguration.class
})
class OfferCacheIntegrationTest {

    private static final UUID OFFER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_OFFER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SECOND_PHOTOGRAPHER_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID NEW_OFFER_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID NEW_PHOTO_ID =
            UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OfferService offerService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        this.cacheManager.getCacheNames().stream()
                .map(this.cacheManager::getCache)
                .forEach(Cache::clear);

        reset(this.offerRepository);
        reset(this.photoRepository);
        reset(this.userRepository);
    }

    @Test
    void cacheManagerShouldExposeAllOfferCaches() {
        assertTrue(this.cacheManager.getCacheNames().containsAll(List.of(
                CacheNames.OFFER_CATALOG,
                CacheNames.OFFER_DETAILS,
                CacheNames.PHOTOGRAPHER_OFFERS
        )));
    }

    @Test
    void repeatedCatalogueReadsShouldUseCachedResult() {
        final List<Offer> offers = List.of(offer(OFFER_ID, PHOTOGRAPHER_ID, "Portrait Session"));

        when(this.offerRepository.findAll()).thenReturn(offers);

        final List<OfferDTO> firstResult = this.offerService.getAllOffers();
        final List<OfferDTO> secondResult = this.offerService.getAllOffers();

        assertSame(firstResult, secondResult);

        verify(this.offerRepository).findAll();
    }

    @Test
    void offerDetailCacheShouldKeepSeparateEntriesForEachIdentifier() {
        final Offer firstOffer = offer(OFFER_ID, PHOTOGRAPHER_ID, "Portrait Session");
        final Offer secondOffer = offer(SECOND_OFFER_ID, PHOTOGRAPHER_ID, "Wedding Session");

        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(firstOffer));
        when(this.offerRepository.findById(SECOND_OFFER_ID)).thenReturn(Optional.of(secondOffer));

        assertEquals(OFFER_ID, this.offerService.getOfferById(OFFER_ID).getId());
        assertEquals(SECOND_OFFER_ID, this.offerService.getOfferById(SECOND_OFFER_ID).getId());
        assertEquals(OFFER_ID, this.offerService.getOfferById(OFFER_ID).getId());

        verify(this.offerRepository).findById(OFFER_ID);
        verify(this.offerRepository).findById(SECOND_OFFER_ID);
    }

    @Test
    void photographerCacheShouldKeepSeparateEntriesForEachPhotographer() {
        when(this.offerRepository.findAllByPhotographerId(PHOTOGRAPHER_ID))
                .thenReturn(List.of(offer(OFFER_ID, PHOTOGRAPHER_ID, "Portrait Session")));
        when(this.offerRepository.findAllByPhotographerId(SECOND_PHOTOGRAPHER_ID))
                .thenReturn(List.of(offer(
                        SECOND_OFFER_ID,
                        SECOND_PHOTOGRAPHER_ID,
                        "Wedding Session"
                )));

        this.offerService.getOffersByPhotographer(PHOTOGRAPHER_ID);
        this.offerService.getOffersByPhotographer(SECOND_PHOTOGRAPHER_ID);
        this.offerService.getOffersByPhotographer(PHOTOGRAPHER_ID);

        verify(this.offerRepository).findAllByPhotographerId(PHOTOGRAPHER_ID);
        verify(this.offerRepository).findAllByPhotographerId(SECOND_PHOTOGRAPHER_ID);
    }

    @Test
    void offerCreationShouldEvictAllOfferCaches() {
        final Offer existingOffer = offer(OFFER_ID, PHOTOGRAPHER_ID, "Portrait Session");
        final User photographer = photographer(PHOTOGRAPHER_ID);
        final Offer savedOffer = offer(NEW_OFFER_ID, PHOTOGRAPHER_ID, "Family Portrait Session");
        final Photo savedPhoto = Photo.builder()
                .id(NEW_PHOTO_ID)
                .offer(savedOffer)
                .build();

        stubOfferReads(existingOffer);

        when(this.userRepository.findById(PHOTOGRAPHER_ID)).thenReturn(Optional.of(photographer));
        when(this.offerRepository.save(any(Offer.class))).thenReturn(savedOffer);
        when(this.photoRepository.save(any(Photo.class))).thenReturn(savedPhoto);

        populateAllOfferCaches();
        this.offerService.createOffer(validOfferRequest(), PHOTOGRAPHER_ID);
        populateAllOfferCaches();

        verifyAllOfferReadsExecutedTwice();
    }

    @Test
    void profileUpdateShouldEvictAllOfferCaches() {
        final Offer existingOffer = offer(OFFER_ID, PHOTOGRAPHER_ID, "Portrait Session");
        final User photographer = photographer(PHOTOGRAPHER_ID);

        stubOfferReads(existingOffer);

        when(this.userRepository.findById(PHOTOGRAPHER_ID)).thenReturn(Optional.of(photographer));
        when(this.userRepository.findByEmailIgnoreCase("updated@example.com"))
                .thenReturn(Optional.of(photographer));

        populateAllOfferCaches();
        this.userService.updateProfile(PHOTOGRAPHER_ID, validProfileUpdate());
        populateAllOfferCaches();

        verifyAllOfferReadsExecutedTwice();
    }

    @Test
    void failedProfileUpdateShouldPreserveExistingCaches() {
        final Offer existingOffer = offer(OFFER_ID, PHOTOGRAPHER_ID, "Portrait Session");
        final ProfileUpdateDTO invalidUpdate = ProfileUpdateDTO.builder()
                .firstName(" ")
                .lastName("Photographer")
                .email("photographer@example.com")
                .build();

        stubOfferReads(existingOffer);
        populateAllOfferCaches();

        assertThrows(
                ProfileUpdateException.class,
                () -> this.userService.updateProfile(PHOTOGRAPHER_ID, invalidUpdate)
        );

        populateAllOfferCaches();

        verify(this.offerRepository).findAll();
        verify(this.offerRepository).findById(OFFER_ID);
        verify(this.offerRepository).findAllByPhotographerId(PHOTOGRAPHER_ID);
    }

    private void stubOfferReads(final Offer existingOffer) {
        when(this.offerRepository.findAll()).thenReturn(List.of(existingOffer));
        when(this.offerRepository.findById(OFFER_ID)).thenReturn(Optional.of(existingOffer));
        when(this.offerRepository.findAllByPhotographerId(PHOTOGRAPHER_ID))
                .thenReturn(List.of(existingOffer));
    }

    private void populateAllOfferCaches() {
        this.offerService.getAllOffers();
        this.offerService.getOfferById(OFFER_ID);
        this.offerService.getOffersByPhotographer(PHOTOGRAPHER_ID);
    }

    private void verifyAllOfferReadsExecutedTwice() {
        verify(this.offerRepository, times(2)).findAll();
        verify(this.offerRepository, times(2)).findById(OFFER_ID);
        verify(this.offerRepository, times(2)).findAllByPhotographerId(PHOTOGRAPHER_ID);
    }

    private static OfferRequestDTO validOfferRequest() {
        return OfferRequestDTO.builder()
                .title("Family Portrait Session")
                .description("A relaxed outdoor family portrait photography session.")
                .price(new BigDecimal("250.00"))
                .durationHours(2)
                .location("Sofia")
                .coverPhotoImageUrl("https://example.com/cover.jpg")
                .available(true)
                .build();
    }

    private static ProfileUpdateDTO validProfileUpdate() {
        return ProfileUpdateDTO.builder()
                .firstName("Updated")
                .lastName("Photographer")
                .email("updated@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
    }

    private static Offer offer(
            final UUID offerId,
            final UUID photographerId,
            final String title) {

        return Offer.builder()
                .id(offerId)
                .title(title)
                .description("A complete professional photography service description.")
                .price(new BigDecimal("200.00"))
                .durationHours(2)
                .location("Sofia")
                .isAvailable(true)
                .photographer(photographer(photographerId))
                .photos(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();
    }

    private static User photographer(final UUID photographerId) {
        return User.builder()
                .id(photographerId)
                .firstName("Photo")
                .lastName("Grapher")
                .username("photographer")
                .email("photographer@example.com")
                .password("hashedPassword")
                .role(UserRole.PHOTOGRAPHER)
                .isActive(true)
                .build();
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        OfferRepository offerRepository() {
            return mock(OfferRepository.class);
        }

        @Bean
        PhotoRepository photoRepository() {
            return mock(PhotoRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        UserService userService(
                final PasswordEncoder passwordEncoder,
                final UserRepository userRepository) {

            return new UserService(passwordEncoder, userRepository);
        }

        @Bean
        OfferService offerService(
                final OfferRepository offerRepository,
                final PhotoRepository photoRepository,
                final UserService userService) {

            return new OfferService(offerRepository, photoRepository, userService);
        }
    }
}
