package photomarketplace.customoffer.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import photomarketplace.customoffer.model.entity.CustomOfferRequest;
import photomarketplace.customoffer.model.entity.CustomOfferStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class CustomOfferRequestRepositoryIntegrationTest {

    @Autowired
    private CustomOfferRequestRepository customOfferRequestRepository;

    @Test
    void saveShouldGenerateUuidAndTimestamps() {
        final CustomOfferRequest savedCustomOffer = saveCustomOffer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID());

        assertNotNull(savedCustomOffer.getId());
        assertNotNull(savedCustomOffer.getCreatedAt());
        assertNotNull(savedCustomOffer.getUpdatedAt());
        assertEquals(CustomOfferStatus.PENDING, savedCustomOffer.getStatus());
    }

    @Test
    void existsShouldMatchClientOfferAndStatus() {
        final UUID clientId = UUID.randomUUID();
        final UUID offerId = UUID.randomUUID();
        saveCustomOffer(clientId, UUID.randomUUID(), offerId);

        assertTrue(this.customOfferRequestRepository.existsByClientIdAndOfferIdAndStatus(
                clientId, offerId, CustomOfferStatus.PENDING));
        assertFalse(this.customOfferRequestRepository.existsByClientIdAndOfferIdAndStatus(
                clientId, offerId, CustomOfferStatus.ACCEPTED));
        assertFalse(this.customOfferRequestRepository.existsByClientIdAndOfferIdAndStatus(
                clientId, UUID.randomUUID(), CustomOfferStatus.PENDING));
    }

    @Test
    void findForClientShouldFilterAndOrderResults() {
        final UUID clientId = UUID.randomUUID();
        final CustomOfferRequest firstCustomOffer = saveCustomOffer(
                clientId, UUID.randomUUID(), UUID.randomUUID());
        final CustomOfferRequest secondCustomOffer = saveCustomOffer(
                clientId, UUID.randomUUID(), UUID.randomUUID());
        saveCustomOffer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        final List<CustomOfferRequest> results =
                this.customOfferRequestRepository.findAllByClientIdOrderByCreatedAtDesc(clientId);

        assertEquals(2, results.size());
        assertEquals(
                Set.of(firstCustomOffer.getId(), secondCustomOffer.getId()),
                results.stream().map(CustomOfferRequest::getId).collect(Collectors.toSet())
        );
        assertTrue(results.get(0).getCreatedAt().compareTo(results.get(1).getCreatedAt()) >= 0);
    }

    @Test
    void findForPhotographerShouldFilterAndOrderResults() {
        final UUID photographerId = UUID.randomUUID();
        final CustomOfferRequest firstCustomOffer = saveCustomOffer(
                UUID.randomUUID(), photographerId, UUID.randomUUID());
        final CustomOfferRequest secondCustomOffer = saveCustomOffer(
                UUID.randomUUID(), photographerId, UUID.randomUUID());
        saveCustomOffer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        final List<CustomOfferRequest> results =
                this.customOfferRequestRepository.findAllByPhotographerIdOrderByCreatedAtDesc(photographerId);

        assertEquals(2, results.size());
        assertEquals(
                Set.of(firstCustomOffer.getId(), secondCustomOffer.getId()),
                results.stream().map(CustomOfferRequest::getId).collect(Collectors.toSet())
        );
        assertTrue(results.get(0).getCreatedAt().compareTo(results.get(1).getCreatedAt()) >= 0);
    }

    private CustomOfferRequest saveCustomOffer(final UUID clientId,
                                               final UUID photographerId,
                                               final UUID offerId) {

        return this.customOfferRequestRepository.saveAndFlush(CustomOfferRequest.builder()
                .clientId(clientId)
                .photographerId(photographerId)
                .offerId(offerId)
                .eventDate(LocalDate.now().plusYears(1))
                .location("Sofia")
                .message("Outdoor portrait photography session")
                .build());
    }
}
