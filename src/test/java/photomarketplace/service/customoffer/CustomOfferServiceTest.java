package photomarketplace.service.customoffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.client.customoffer.CustomOfferClient;
import photomarketplace.exception.customoffer.CustomOfferIntegrationException;
import photomarketplace.exception.customoffer.CustomOfferNotFoundException;
import photomarketplace.exception.customoffer.CustomOfferOperationException;
import photomarketplace.exception.customoffer.CustomOfferServiceUnavailableException;
import photomarketplace.model.dto.customoffer.CreateCustomOfferRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionDTO;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferResponseDTO;
import photomarketplace.model.dto.customoffer.CustomOfferStatusDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.user.UserService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOfferServiceTest {

    private static final UUID CUSTOM_OFFER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate EVENT_DATE = LocalDate.of(2030, 8, 15);
    private static final BigDecimal PROPOSED_PRICE = new BigDecimal("450.00");

    @Mock
    private CustomOfferClient customOfferClient;

    @Mock
    private OfferService offerService;

    @Mock
    private UserService userService;

    private CustomOfferService customOfferService;

    @BeforeEach
    void setUp() {
        this.customOfferService = new CustomOfferService(
                this.customOfferClient,
                this.offerService,
                this.userService,
                new ObjectMapper());
    }

    @Test
    void createShouldValidateMapAndDelegateRequest() {
        final CustomOfferRequestDTO request = CustomOfferRequestDTO.builder()
                .eventDate(EVENT_DATE)
                .location("  Sofia  ")
                .message("  Outdoor portrait photography session  ")
                .build();

        final CreateCustomOfferRequestDTO clientRequest = new CreateCustomOfferRequestDTO(
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                OFFER_ID,
                EVENT_DATE,
                "Sofia",
                "Outdoor portrait photography session");

        final CustomOfferResponseDTO expectedResponse = response(CustomOfferStatusDTO.PENDING, null);

        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer(true));
        when(this.customOfferClient.create(clientRequest)).thenReturn(expectedResponse);

        final CustomOfferResponseDTO actualResponse =
                this.customOfferService.createCustomOffer(OFFER_ID, CLIENT_ID, request);

        assertSame(expectedResponse, actualResponse);

        verify(this.customOfferClient).create(clientRequest);
    }

    @Test
    void createShouldRejectNonClientUser() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.PHOTOGRAPHER));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.createCustomOffer(OFFER_ID, CLIENT_ID, validRequest())
        );

        assertEquals("Only users with the CLIENT role can perform this operation.", exception.getMessage());

        verifyNoInteractions(this.offerService, this.customOfferClient);
    }

    @Test
    void createShouldRejectIncompleteRequest() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));

        final CustomOfferRequestDTO request = CustomOfferRequestDTO.builder()
                .eventDate(EVENT_DATE)
                .location(" ")
                .message("Valid message")
                .build();

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.createCustomOffer(OFFER_ID, CLIENT_ID, request)
        );

        assertEquals("Complete custom offer request details are required.", exception.getMessage());

        verifyNoInteractions(this.offerService, this.customOfferClient);
    }

    @Test
    void createShouldRejectUnavailableOffer() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer(false));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.createCustomOffer(OFFER_ID, CLIENT_ID, validRequest())
        );

        assertEquals("Custom offers cannot be requested for an unavailable offer.", exception.getMessage());

        verifyNoInteractions(this.customOfferClient);
    }

    @Test
    void getClientCustomOffersShouldDelegateForClient() {
        final List<CustomOfferResponseDTO> expectedResponses =
                List.of(response(CustomOfferStatusDTO.PENDING, null));

        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID)).thenReturn(expectedResponses);

        final List<CustomOfferResponseDTO> actualResponses =
                this.customOfferService.getClientCustomOffers(CLIENT_ID);

        assertSame(expectedResponses, actualResponses);
    }

    @Test
    void getPhotographerCustomOffersShouldDelegateForPhotographer() {
        final List<CustomOfferResponseDTO> expectedResponses =
                List.of(response(CustomOfferStatusDTO.PENDING, null));

        when(this.userService.getUser(PHOTOGRAPHER_ID)).thenReturn(user(UserRole.PHOTOGRAPHER));
        when(this.customOfferClient.getForPhotographer(PHOTOGRAPHER_ID)).thenReturn(expectedResponses);

        final List<CustomOfferResponseDTO> actualResponses =
                this.customOfferService.getPhotographerCustomOffers(PHOTOGRAPHER_ID);

        assertSame(expectedResponses, actualResponses);
    }

    @Test
    void decideShouldAcceptWithPositivePrice() {
        final CustomOfferDecisionRequestDTO request = new CustomOfferDecisionRequestDTO(
                CustomOfferDecisionDTO.ACCEPT,
                PROPOSED_PRICE
        );

        final CustomOfferResponseDTO expectedResponse =
                response(CustomOfferStatusDTO.ACCEPTED, PROPOSED_PRICE);

        when(this.userService.getUser(PHOTOGRAPHER_ID)).thenReturn(user(UserRole.PHOTOGRAPHER));
        when(this.customOfferClient.decide(CUSTOM_OFFER_ID, PHOTOGRAPHER_ID, request))
                .thenReturn(expectedResponse);

        final CustomOfferResponseDTO actualResponse = this.customOfferService.decideCustomOffer(
                CUSTOM_OFFER_ID,
                PHOTOGRAPHER_ID,
                request
        );

        assertSame(expectedResponse, actualResponse);
    }

    @Test
    void decideShouldRemovePriceWhenDeclining() {
        final CustomOfferDecisionRequestDTO request = new CustomOfferDecisionRequestDTO(
                CustomOfferDecisionDTO.DECLINE,
                PROPOSED_PRICE
        );

        final CustomOfferDecisionRequestDTO expectedClientRequest = new CustomOfferDecisionRequestDTO(
                CustomOfferDecisionDTO.DECLINE,
                null
        );

        final CustomOfferResponseDTO expectedResponse = response(CustomOfferStatusDTO.DECLINED, null);

        when(this.userService.getUser(PHOTOGRAPHER_ID)).thenReturn(user(UserRole.PHOTOGRAPHER));
        when(this.customOfferClient.decide(CUSTOM_OFFER_ID, PHOTOGRAPHER_ID, expectedClientRequest))
                .thenReturn(expectedResponse);

        final CustomOfferResponseDTO actualResponse = this.customOfferService.decideCustomOffer(
                CUSTOM_OFFER_ID,
                PHOTOGRAPHER_ID,
                request
        );

        assertSame(expectedResponse, actualResponse);

        verify(this.customOfferClient).decide(CUSTOM_OFFER_ID, PHOTOGRAPHER_ID, expectedClientRequest);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void decideShouldRejectMissingDecision() {
        when(this.userService.getUser(PHOTOGRAPHER_ID)).thenReturn(user(UserRole.PHOTOGRAPHER));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.decideCustomOffer(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(null, null))
        );

        assertEquals("A custom offer decision is required.", exception.getMessage());

        verifyNoInteractions(this.customOfferClient);
    }

    @Test
    void decideShouldRejectNonPositiveAcceptedPrice() {
        when(this.userService.getUser(PHOTOGRAPHER_ID)).thenReturn(user(UserRole.PHOTOGRAPHER));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.decideCustomOffer(
                        CUSTOM_OFFER_ID,
                        PHOTOGRAPHER_ID,
                        new CustomOfferDecisionRequestDTO(
                                CustomOfferDecisionDTO.ACCEPT,
                                BigDecimal.ZERO)));

        assertEquals("An accepted custom offer must include a positive price.", exception.getMessage());

        verifyNoInteractions(this.customOfferClient);
    }

    @Test
    void withdrawShouldDelegateForClient() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));

        this.customOfferService.withdrawCustomOffer(CUSTOM_OFFER_ID, CLIENT_ID);

        verify(this.customOfferClient).withdraw(CUSTOM_OFFER_ID, CLIENT_ID);
    }

    @Test
    void remoteNotFoundShouldPreserveRemoteMessage() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID)).thenThrow(feignException(
                404,
                "{\"message\":\"Custom offer request does not exist.\"}")
        );

        final CustomOfferNotFoundException exception = assertThrows(
                CustomOfferNotFoundException.class,
                () -> this.customOfferService.getClientCustomOffers(CLIENT_ID)
        );

        assertEquals("Custom offer request does not exist.", exception.getMessage());
    }

    @Test
    void remoteConflictShouldBecomeOperationException() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID)).thenThrow(feignException(
                409,
                "{\"message\":\"Only pending requests can be changed.\"}")
        );

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.getClientCustomOffers(CLIENT_ID)
        );

        assertEquals("Only pending requests can be changed.", exception.getMessage());
    }

    @Test
    void malformedRemoteErrorShouldUseFallbackMessage() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID))
                .thenThrow(feignException(400, "not-json"));

        final CustomOfferOperationException exception = assertThrows(
                CustomOfferOperationException.class,
                () -> this.customOfferService.getClientCustomOffers(CLIENT_ID)
        );

        assertEquals("The custom offer request could not be completed.", exception.getMessage());
    }

    @Test
    void remoteServerErrorShouldBecomeUnavailableException() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID))
                .thenThrow(feignException(503, ""));

        assertThrows(
                CustomOfferServiceUnavailableException.class,
                () -> this.customOfferService.getClientCustomOffers(CLIENT_ID)
        );
    }

    @Test
    void unexpectedRemoteStatusShouldBecomeIntegrationException() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID))
                .thenThrow(feignException(403, "{}"));

        assertThrows(
                CustomOfferIntegrationException.class,
                () -> this.customOfferService.getClientCustomOffers(CLIENT_ID)
        );
    }

    @Test
    void connectionFailureShouldBecomeUnavailableException() {
        when(this.userService.getUser(CLIENT_ID)).thenReturn(user(UserRole.CLIENT));
        when(this.customOfferClient.getForClient(CLIENT_ID)).thenThrow(new RetryableException(
                -1,
                "Connection refused",
                Request.HttpMethod.GET,
                new IOException("Connection refused"),
                (Long) null,
                request())
        );

        assertThrows(
                CustomOfferServiceUnavailableException.class,
                () -> this.customOfferService.getClientCustomOffers(CLIENT_ID)
        );
    }

    private static User user(final UserRole role) {
        return User.builder()
                .id(role == UserRole.CLIENT ? CLIENT_ID : PHOTOGRAPHER_ID)
                .role(role)
                .build();
    }

    private static OfferDTO offer(final boolean available) {
        return OfferDTO.builder()
                .id(OFFER_ID)
                .isAvailable(available)
                .photographer(UserDTO.builder().id(PHOTOGRAPHER_ID).build())
                .build();
    }

    private static CustomOfferRequestDTO validRequest() {
        return CustomOfferRequestDTO.builder()
                .eventDate(EVENT_DATE)
                .location("Sofia")
                .message("Outdoor portrait photography session")
                .build();
    }

    private static CustomOfferResponseDTO response(final CustomOfferStatusDTO status,
                                                   final BigDecimal proposedPrice) {

        return new CustomOfferResponseDTO(
                CUSTOM_OFFER_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                OFFER_ID,
                EVENT_DATE,
                "Sofia",
                "Outdoor portrait photography session",
                proposedPrice,
                status,
                LocalDateTime.of(2026, 8, 6, 10, 0),
                LocalDateTime.of(2026, 8, 6, 10, 0)
        );
    }

    private static FeignException feignException(final int status, final String responseBody) {
        final Response response = Response.builder()
                .status(status)
                .reason("Remote error")
                .request(request())
                .headers(Map.of())
                .body(responseBody, StandardCharsets.UTF_8)
                .build();

        return FeignException.errorStatus("CustomOfferClient#request", response);
    }

    private static Request request() {
        return Request.create(
                Request.HttpMethod.GET,
                "http://localhost:8081/api/custom-offers",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
    }
}
