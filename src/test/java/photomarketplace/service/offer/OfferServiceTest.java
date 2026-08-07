package photomarketplace.service.offer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.model.entity.offer.Offer;
import photomarketplace.repository.offer.OfferRepository;
import photomarketplace.repository.photo.PhotoRepository;
import photomarketplace.service.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UserService userService;

    private OfferService offerService;

    @BeforeEach
    void setUp() {
        this.offerService = new OfferService(
                this.offerRepository,
                this.photoRepository,
                this.userService
        );
    }

    @Test
    void expireStaleOffersShouldMakeOffersUnavailableAndPersistThem() {
        final LocalDateTime cutoff = LocalDateTime.of(2026, 5, 9, 13, 30);
        final Offer firstOffer = Offer.builder().isAvailable(true).build();
        final Offer secondOffer = Offer.builder().isAvailable(true).build();
        final List<Offer> staleOffers = List.of(firstOffer, secondOffer);

        when(this.offerRepository.findAllByIsAvailableTrueAndUpdatedAtBefore(cutoff))
                .thenReturn(staleOffers);

        final int expiredOffers = this.offerService.expireStaleOffers(cutoff);

        assertEquals(2, expiredOffers);
        assertFalse(firstOffer.isAvailable());
        assertFalse(secondOffer.isAvailable());

        verify(this.offerRepository).saveAll(staleOffers);
    }

    @Test
    void expireStaleOffersShouldNotPersistWhenNoOffersAreStale() {
        final LocalDateTime cutoff = LocalDateTime.of(2026, 5, 9, 13, 30);

        when(this.offerRepository.findAllByIsAvailableTrueAndUpdatedAtBefore(cutoff))
                .thenReturn(List.of());

        final int expiredOffers = this.offerService.expireStaleOffers(cutoff);

        assertEquals(0, expiredOffers);

        verify(this.offerRepository, never()).saveAll(anyList());
    }

    @Test
    void expireStaleOffersShouldRejectMissingCutoff() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.offerService.expireStaleOffers(null)
        );

        assertEquals("Offer expiration cutoff is required.", exception.getMessage());

        verifyNoInteractions(this.offerRepository);
    }
}
