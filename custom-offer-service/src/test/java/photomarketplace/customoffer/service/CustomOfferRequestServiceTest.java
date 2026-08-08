package photomarketplace.customoffer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.customoffer.exception.CustomOfferNotFoundException;
import photomarketplace.customoffer.exception.CustomOfferOperationException;
import photomarketplace.customoffer.model.dto.customoffer.CreateCustomOfferRequestDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferDecisionDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferResponseDTO;
import photomarketplace.customoffer.model.entity.CustomOfferRequest;
import photomarketplace.customoffer.model.entity.CustomOfferStatus;
import photomarketplace.customoffer.repository.CustomOfferRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOfferRequestServiceTest {

    private static final UUID CUSTOM_OFFER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate EVENT_DATE = LocalDate.of(2030, 8, 15);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 6, 10, 0);

    @Mock
    private CustomOfferRequestRepository customOfferRequestRepository;

    @InjectMocks
    private CustomOfferRequestService customOfferRequestService;

    @Test
    void createShouldSavePendingCustomOffer() {
        final CreateCustomOfferRequestDTO request = createRequest();

        when(this.customOfferRequestRepository.save(any(CustomOfferRequest.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        final CustomOfferResponseDTO response = this.customOfferRequestService.create(request);

        final ArgumentCaptor<CustomOfferRequest> customOfferCaptor =
                ArgumentCaptor.forClass(CustomOfferRequest.class);

        verify(this.customOfferRequestRepository).save(customOfferCaptor.capture());

        final CustomOfferRequest savedCustomOffer = customOfferCaptor.getValue();

        assertAll(
                () -> assertEquals(CUSTOM_OFFER_ID, response.id()),
                () -> assertEquals(CustomOfferStatus.PENDING, response.status()),
                () -> assertEquals("Sofia", savedCustomOffer.getLocation()),
                () -> assertEquals("Outdoor portrait photography session", savedCustomOffer.getMessage()),
                () -> assertNull(savedCustomOffer.getProposedPrice())
        );
    }

    @Test
    void createShouldRejectDuplicatePendingRequest() {
        final CreateCustomOfferRequestDTO request = createRequest();

        when(this.customOfferRequestRepository.existsByClientIdAndOfferIdAndStatus(
                CLIENT_ID, OFFER_ID, CustomOfferStatus.PENDING)).thenReturn(true);

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.create(request));

        assertEquals("A pending custom offer request already exists for this offer.", exception.getMessage());

        verify(this.customOfferRequestRepository, never()).save(any(CustomOfferRequest.class));
    }

    @Test
    void createShouldRejectMissingRequestDetails() {
        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.create(null));

        assertEquals("Complete custom offer request details are required.", exception.getMessage());

        verifyNoInteractions(this.customOfferRequestRepository);
    }

    @Test
    void createShouldRejectNonFutureEventDate() {
        final CreateCustomOfferRequestDTO request = createRequest(
                LocalDate.now(),
                "Sofia",
                "Outdoor portrait photography session");

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.create(request));

        assertEquals("The custom offer event date must be in the future.", exception.getMessage());

        verifyNoInteractions(this.customOfferRequestRepository);
    }

    @Test
    void createShouldValidateTrimmedMessageLength() {
        final CreateCustomOfferRequestDTO request = createRequest(
                EVENT_DATE,
                "Sofia",
                "  Too short  ");

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.create(request));

        assertEquals("The custom offer message must be between 10 and 2000 characters.",
                exception.getMessage());

        verifyNoInteractions(this.customOfferRequestRepository);
    }

    @Test
    void createShouldRejectOversizedLocation() {
        final CreateCustomOfferRequestDTO request = createRequest(
                EVENT_DATE,
                "S".repeat(256),
                "Outdoor portrait photography session");

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.create(request));

        assertEquals("The custom offer location must contain at most 255 characters.",
                exception.getMessage());

        verifyNoInteractions(this.customOfferRequestRepository);
    }

    @Test
    void getByIdShouldReturnExistingCustomOffer() {
        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer(CustomOfferStatus.PENDING)));

        final CustomOfferResponseDTO response = this.customOfferRequestService.getById(CUSTOM_OFFER_ID);

        assertAll(
                () -> assertEquals(CUSTOM_OFFER_ID, response.id()),
                () -> assertEquals(CLIENT_ID, response.clientId()),
                () -> assertEquals(PHOTOGRAPHER_ID, response.photographerId())
        );
    }

    @Test
    void getByIdShouldRejectUnknownCustomOffer() {
        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID)).thenReturn(Optional.empty());

        final CustomOfferNotFoundException exception = assertThrows(
                CustomOfferNotFoundException.class,
                () -> this.customOfferRequestService.getById(CUSTOM_OFFER_ID));

        assertEquals("Custom offer request with id '%s' does not exist.".formatted(CUSTOM_OFFER_ID),
                exception.getMessage()
        );
    }

    @Test
    void listMethodsShouldMapRepositoryResults() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findAllByClientIdOrderByCreatedAtDesc(CLIENT_ID))
                .thenReturn(List.of(customOffer));
        when(this.customOfferRequestRepository.findAllByPhotographerIdOrderByCreatedAtDesc(PHOTOGRAPHER_ID))
                .thenReturn(List.of(customOffer));

        final List<CustomOfferResponseDTO> clientOffers =
                this.customOfferRequestService.getForClient(CLIENT_ID);
        final List<CustomOfferResponseDTO> photographerOffers =
                this.customOfferRequestService.getForPhotographer(PHOTOGRAPHER_ID);

        assertAll(
                () -> assertEquals(List.of(CUSTOM_OFFER_ID),
                        clientOffers.stream().map(CustomOfferResponseDTO::id).toList()),
                () -> assertEquals(List.of(CUSTOM_OFFER_ID),
                        photographerOffers.stream().map(CustomOfferResponseDTO::id).toList())
        );
    }

    @Test
    void decideShouldAcceptPendingRequestWithPrice() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);
        final BigDecimal proposedPrice = new BigDecimal("450.00");

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));
        when(this.customOfferRequestRepository.save(customOffer)).thenReturn(customOffer);

        final CustomOfferResponseDTO response = this.customOfferRequestService.decide(
                CUSTOM_OFFER_ID,
                PHOTOGRAPHER_ID,
                new CustomOfferDecisionRequestDTO(CustomOfferDecisionDTO.ACCEPT, proposedPrice));

        assertAll(
                () -> assertEquals(CustomOfferStatus.ACCEPTED, response.status()),
                () -> assertEquals(proposedPrice, response.proposedPrice())
        );
    }

    @Test
    void decideShouldDeclinePendingRequest() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);
        customOffer.setProposedPrice(new BigDecimal("350.00"));

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));
        when(this.customOfferRequestRepository.save(customOffer)).thenReturn(customOffer);

        final CustomOfferResponseDTO response = this.customOfferRequestService.decide(
                CUSTOM_OFFER_ID,
                PHOTOGRAPHER_ID,
                new CustomOfferDecisionRequestDTO(CustomOfferDecisionDTO.DECLINE, null));

        assertAll(
                () -> assertEquals(CustomOfferStatus.DECLINED, response.status()),
                () -> assertNull(response.proposedPrice())
        );
    }

    @Test
    void decideShouldRequirePriceWhenAccepting() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.decide(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(CustomOfferDecisionDTO.ACCEPT, null)));

        assertEquals("An accepted custom offer must include a proposed price.", exception.getMessage());

        verify(this.customOfferRequestRepository, never()).save(any(CustomOfferRequest.class));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void decideShouldRequireDecision() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.decide(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(null, null)));

        assertEquals("A custom offer decision is required.", exception.getMessage());

        verify(this.customOfferRequestRepository, never()).save(any(CustomOfferRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void decideShouldRejectNonPositivePrice(final String price) {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.decide(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(
                                CustomOfferDecisionDTO.ACCEPT,
                                new BigDecimal(price))));

        assertEquals("The proposed price must be positive.", exception.getMessage());

        verify(this.customOfferRequestRepository, never()).save(any(CustomOfferRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789.00", "10.001"})
    void decideShouldRejectInvalidPriceDigits(final String price) {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.decide(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(
                                CustomOfferDecisionDTO.ACCEPT,
                                new BigDecimal(price))));

        assertEquals("The proposed price must have up to 8 digits and 2 decimals.",
                exception.getMessage());

        verify(this.customOfferRequestRepository, never()).save(any(CustomOfferRequest.class));
    }

    @Test
    void decideShouldRejectDifferentPhotographer() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.decide(
                        CUSTOM_OFFER_ID,
                        UUID.randomUUID(),
                        new CustomOfferDecisionRequestDTO(CustomOfferDecisionDTO.DECLINE, null)));

        assertEquals("Only the custom offer photographer can decide this request.", exception.getMessage());
    }

    @Test
    void decideShouldRejectCompletedRequest() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.ACCEPTED);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.decide(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(CustomOfferDecisionDTO.DECLINE, null)));

        assertEquals("Only pending custom offer requests can be changed.", exception.getMessage());
    }

    @Test
    void withdrawShouldChangePendingRequestStatus() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        this.customOfferRequestService.withdraw(CUSTOM_OFFER_ID, CLIENT_ID);

        assertEquals(CustomOfferStatus.WITHDRAWN, customOffer.getStatus());

        verify(this.customOfferRequestRepository).save(customOffer);
    }

    @Test
    void withdrawShouldRejectDifferentClient() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.PENDING);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.withdraw(CUSTOM_OFFER_ID, UUID.randomUUID()));

        assertEquals("Only the custom offer client can withdraw this request.", exception.getMessage());
    }

    @Test
    void withdrawShouldRejectCompletedRequest() {
        final CustomOfferRequest customOffer = customOffer(CustomOfferStatus.DECLINED);

        when(this.customOfferRequestRepository.findById(CUSTOM_OFFER_ID))
                .thenReturn(Optional.of(customOffer));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferRequestService.withdraw(CUSTOM_OFFER_ID, CLIENT_ID));

        assertEquals("Only pending custom offer requests can be changed.", exception.getMessage());
    }

    private static CreateCustomOfferRequestDTO createRequest() {
        return createRequest(
                EVENT_DATE,
                "  Sofia  ",
                "  Outdoor portrait photography session  ");
    }

    private static CreateCustomOfferRequestDTO createRequest(
            final LocalDate eventDate,
            final String location,
            final String message) {

        return new CreateCustomOfferRequestDTO(
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                OFFER_ID,
                eventDate,
                location,
                message);
    }

    private static CustomOfferRequest customOffer(final CustomOfferStatus status) {
        return CustomOfferRequest.builder()
                .id(CUSTOM_OFFER_ID)
                .clientId(CLIENT_ID)
                .photographerId(PHOTOGRAPHER_ID)
                .offerId(OFFER_ID)
                .eventDate(EVENT_DATE)
                .location("Sofia")
                .message("Outdoor portrait photography session")
                .status(status)
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }

    private static CustomOfferRequest persisted(final CustomOfferRequest customOffer) {
        customOffer.setId(CUSTOM_OFFER_ID);
        customOffer.setCreatedAt(CREATED_AT);
        customOffer.setUpdatedAt(CREATED_AT);
        return customOffer;
    }
}
