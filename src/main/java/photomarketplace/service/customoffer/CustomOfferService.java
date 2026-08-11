package photomarketplace.service.customoffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.RetryableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
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
import photomarketplace.model.dto.customoffer.PhotographerCustomOfferViewDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.offer.OfferService;
import photomarketplace.service.user.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Validated
public class CustomOfferService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomOfferService.class);
    private static final String DEFAULT_ERROR_MESSAGE =
            "The custom offer request could not be completed.";
    private static final String UNAVAILABLE_MESSAGE =
            "The custom offer service is temporarily unavailable. Please try again later.";

    private final CustomOfferClient customOfferClient;
    private final OfferService offerService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public CustomOfferService(final CustomOfferClient customOfferClient,
                              final OfferService offerService,
                              final UserService userService,
                              final ObjectMapper objectMapper) {

        this.customOfferClient = customOfferClient;
        this.offerService = offerService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public CustomOfferResponseDTO createCustomOffer(final UUID offerId,
                                                    final UUID clientId,
                                                    @Valid @NotNull final CustomOfferRequestDTO request) {

        requireUserRole(clientId, UserRole.CLIENT);
        requireRequest(request);

        final OfferDTO offer = this.offerService.getOfferById(offerId);

        if (!offer.isAvailable()) {
            throw new CustomOfferOperationException("Custom offers cannot be requested for an unavailable offer.");
        }

        final CreateCustomOfferRequestDTO clientRequest = new CreateCustomOfferRequestDTO(
                clientId,
                offer.getPhotographer().getId(),
                offerId,
                request.getEventDate(),
                request.getLocation().trim(),
                request.getMessage().trim());

        final CustomOfferResponseDTO response = execute(() -> this.customOfferClient.create(clientRequest));

        LOGGER.info("Client {} created custom offer request {} for offer {}.",
                clientId, response.id(), offerId);

        return response;
    }

    public List<CustomOfferResponseDTO> getClientCustomOffers(final UUID clientId) {
        requireUserRole(clientId, UserRole.CLIENT);
        return execute(() -> this.customOfferClient.getForClient(clientId));
    }

    public List<CustomOfferResponseDTO> getPhotographerCustomOffers(final UUID photographerId) {
        requireUserRole(photographerId, UserRole.PHOTOGRAPHER);
        return execute(() -> this.customOfferClient.getForPhotographer(photographerId));
    }

    public List<PhotographerCustomOfferViewDTO> getPhotographerCustomOfferViews(final UUID photographerId) {
        return getPhotographerCustomOffers(photographerId).stream()
                .map(this::toPhotographerView)
                .toList();
    }

    public CustomOfferResponseDTO decideCustomOffer(
            final UUID customOfferId,
            final UUID photographerId,
            @Valid @NotNull final CustomOfferDecisionRequestDTO request) {

        requireUserRole(photographerId, UserRole.PHOTOGRAPHER);
        requireDecision(request);

        final CustomOfferDecisionRequestDTO clientRequest = request.decision() == CustomOfferDecisionDTO.DECLINE
                ? new CustomOfferDecisionRequestDTO(CustomOfferDecisionDTO.DECLINE, null)
                : request;

        final CustomOfferResponseDTO response = execute(() -> this.customOfferClient.decide(
                customOfferId,
                photographerId,
                clientRequest)
        );

        LOGGER.info("Photographer {} changed custom offer request {} to {}.",
                photographerId, customOfferId, response.status());

        return response;
    }

    public void withdrawCustomOffer(final UUID customOfferId, final UUID clientId) {
        requireUserRole(clientId, UserRole.CLIENT);

        execute(() -> {
            this.customOfferClient.withdraw(customOfferId, clientId);
            return null;
        });

        LOGGER.info("Client {} withdrew custom offer request {}.", clientId, customOfferId);
    }

    private void requireUserRole(final UUID userId, final UserRole requiredRole) {
        final User user = this.userService.getUser(userId);

        if (user.getRole() != requiredRole) {
            throw new CustomOfferOperationException(
                    "Only users with the %s role can perform this operation.".formatted(requiredRole));
        }
    }

    private static void requireRequest(final CustomOfferRequestDTO request) {
        if (request == null || request.getEventDate() == null
                || request.getLocation() == null
                || request.getLocation().isBlank()
                || request.getMessage() == null
                || request.getMessage().isBlank()) {

            throw new CustomOfferOperationException("Complete custom offer request details are required.");
        }
    }

    private static void requireDecision(final CustomOfferDecisionRequestDTO request) {
        if (request == null || request.decision() == null) {
            throw new CustomOfferOperationException("A custom offer decision is required.");
        }

        final BigDecimal proposedPrice = request.proposedPrice();

        if (request.decision() == CustomOfferDecisionDTO.ACCEPT
                && (proposedPrice == null || proposedPrice.signum() <= 0)) {

            throw new CustomOfferOperationException("An accepted custom offer must include a positive price.");
        }
    }

    private PhotographerCustomOfferViewDTO toPhotographerView(final CustomOfferResponseDTO customOffer) {
        final UserDTO client = this.userService.getUserById(customOffer.clientId());
        final OfferDTO offer = this.offerService.getOfferById(customOffer.offerId());

        return new PhotographerCustomOfferViewDTO(
                customOffer,
                client.getDisplayName(),
                offer.getTitle(),
                offer.getPrice());
    }

    private <T> T execute(final Supplier<T> clientCall) {
        try {
            return clientCall.get();
        } catch (RetryableException exception) {
            LOGGER.error("Custom offer service could not be reached.");
            throw new CustomOfferServiceUnavailableException(UNAVAILABLE_MESSAGE);
        } catch (FeignException exception) {
            throw translate(exception);
        }
    }

    private RuntimeException translate(final FeignException exception) {
        final String message = extractMessage(exception);

        LOGGER.warn("Custom offer service returned HTTP status {}.", exception.status());

        if (exception.status() == 404) {
            return new CustomOfferNotFoundException(message);
        }

        if (exception.status() == 400 || exception.status() == 409) {
            return new CustomOfferOperationException(message);
        }

        if (exception.status() >= 500) {
            return new CustomOfferServiceUnavailableException(UNAVAILABLE_MESSAGE);
        }

        return new CustomOfferIntegrationException(
                "The custom offer request could not be completed. Please try again."
        );
    }

    private String extractMessage(final FeignException exception) {
        try {
            final JsonNode responseBody = this.objectMapper.readTree(exception.contentUTF8());

            if (responseBody == null) {
                return DEFAULT_ERROR_MESSAGE;
            }

            final String message = responseBody.path("message").asText();

            return message.isBlank()
                    ? DEFAULT_ERROR_MESSAGE
                    : message;
        } catch (JsonProcessingException exceptionWhileReadingResponse) {
            return DEFAULT_ERROR_MESSAGE;
        }
    }
}
