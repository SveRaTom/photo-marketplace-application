package photomarketplace.customoffer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomOfferRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomOfferRequestService.class);

    private final CustomOfferRequestRepository customOfferRequestRepository;

    public CustomOfferRequestService(final CustomOfferRequestRepository customOfferRequestRepository) {
        this.customOfferRequestRepository = customOfferRequestRepository;
    }

    public CustomOfferResponseDTO create(final CreateCustomOfferRequestDTO request) {
        requireCreateRequest(request);

        if (this.customOfferRequestRepository.existsByClientIdAndOfferIdAndStatus(
                request.clientId(), request.offerId(), CustomOfferStatus.PENDING)) {
            throw new CustomOfferOperationException("A pending custom offer request already exists for this offer.");
        }

        final CustomOfferRequest customOffer = CustomOfferRequest.builder()
                .clientId(request.clientId())
                .photographerId(request.photographerId())
                .offerId(request.offerId())
                .eventDate(request.eventDate())
                .location(request.location().trim())
                .message(request.message().trim())
                .build();

        final CustomOfferResponseDTO response = CustomOfferResponseDTO.from(
                this.customOfferRequestRepository.save(customOffer));

        LOGGER.info("Created custom offer request {} for offer {} by client {}.",
                response.id(), response.offerId(), response.clientId());

        return response;
    }

    @Transactional(readOnly = true)
    public CustomOfferResponseDTO getById(final UUID customOfferId) {
        return CustomOfferResponseDTO.from(getCustomOffer(customOfferId));
    }

    @Transactional(readOnly = true)
    public List<CustomOfferResponseDTO> getForClient(final UUID clientId) {
        return this.customOfferRequestRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(CustomOfferResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomOfferResponseDTO> getForPhotographer(final UUID photographerId) {
        return this.customOfferRequestRepository.findAllByPhotographerIdOrderByCreatedAtDesc(photographerId).stream()
                .map(CustomOfferResponseDTO::from)
                .toList();
    }

    public CustomOfferResponseDTO decide(final UUID customOfferId,
                                         final UUID photographerId,
                                         final CustomOfferDecisionRequestDTO request) {

        final CustomOfferRequest customOffer = getCustomOffer(customOfferId);
        ensurePhotographerOwnsCustomOffer(customOffer, photographerId);
        ensurePending(customOffer);
        requireDecisionRequest(request);

        if (request.decision() == CustomOfferDecisionDTO.ACCEPT) {
            customOffer.accept(request.proposedPrice());
        } else {
            customOffer.decline();
        }

        final CustomOfferResponseDTO response = CustomOfferResponseDTO.from(
                this.customOfferRequestRepository.save(customOffer));

        LOGGER.info("Custom offer request {} was {} by photographer {}.",
                response.id(), response.status(), photographerId);

        return response;
    }

    public void withdraw(final UUID customOfferId, final UUID clientId) {
        final CustomOfferRequest customOffer = getCustomOffer(customOfferId);

        if (!customOffer.getClientId().equals(clientId)) {
            throw new CustomOfferOperationException("Only the custom offer client can withdraw this request.");
        }

        ensurePending(customOffer);
        customOffer.withdraw();
        this.customOfferRequestRepository.save(customOffer);

        LOGGER.info("Custom offer request {} was withdrawn by client {}.", customOfferId, clientId);
    }

    private static void requireCreateRequest(final CreateCustomOfferRequestDTO request) {
        if (request == null
                || request.clientId() == null
                || request.photographerId() == null
                || request.offerId() == null
                || request.eventDate() == null
                || request.location() == null
                || request.location().isBlank()
                || request.message() == null
                || request.message().isBlank()) {

            throw new CustomOfferOperationException("Complete custom offer request details are required.");
        }

        if (!request.eventDate().isAfter(LocalDate.now())) {
            throw new CustomOfferOperationException("The custom offer event date must be in the future.");
        }

        if (request.location().trim().length() > 255) {
            throw new CustomOfferOperationException(
                    "The custom offer location must contain at most 255 characters.");
        }

        final int messageLength = request.message().trim().length();

        if (messageLength < 10 || messageLength > 2000) {
            throw new CustomOfferOperationException(
                    "The custom offer message must be between 10 and 2000 characters.");
        }
    }

    private static void requireDecisionRequest(final CustomOfferDecisionRequestDTO request) {
        if (request == null || request.decision() == null) {
            throw new CustomOfferOperationException("A custom offer decision is required.");
        }

        final BigDecimal proposedPrice = request.proposedPrice();

        if (request.decision() == CustomOfferDecisionDTO.ACCEPT && proposedPrice == null) {
            throw new CustomOfferOperationException(
                    "An accepted custom offer must include a proposed price.");
        }

        if (proposedPrice != null && proposedPrice.signum() <= 0) {
            throw new CustomOfferOperationException("The proposed price must be positive.");
        }

        if (proposedPrice != null
                && (proposedPrice.scale() > 2 || proposedPrice.precision() - proposedPrice.scale() > 8)) {

            throw new CustomOfferOperationException(
                    "The proposed price must have up to 8 digits and 2 decimals.");
        }
    }

    private CustomOfferRequest getCustomOffer(final UUID customOfferId) {
        return this.customOfferRequestRepository.findById(customOfferId)
                .orElseThrow(() -> new CustomOfferNotFoundException(customOfferId));
    }

    private static void ensurePhotographerOwnsCustomOffer(final CustomOfferRequest customOffer,
                                                          final UUID photographerId) {
        if (!customOffer.getPhotographerId().equals(photographerId)) {
            throw new CustomOfferOperationException("Only the custom offer photographer can decide this request.");
        }
    }

    private static void ensurePending(final CustomOfferRequest customOffer) {
        if (customOffer.getStatus() != CustomOfferStatus.PENDING) {
            throw new CustomOfferOperationException("Only pending custom offer requests can be changed.");
        }
    }
}
