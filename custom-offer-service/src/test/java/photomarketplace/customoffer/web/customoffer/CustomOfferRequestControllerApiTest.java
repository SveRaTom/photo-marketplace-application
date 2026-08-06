package photomarketplace.customoffer.web.customoffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import photomarketplace.customoffer.exception.CustomOfferNotFoundException;
import photomarketplace.customoffer.exception.CustomOfferOperationException;
import photomarketplace.customoffer.model.dto.customoffer.CreateCustomOfferRequestDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferDecisionDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.customoffer.model.dto.customoffer.CustomOfferResponseDTO;
import photomarketplace.customoffer.model.entity.CustomOfferStatus;
import photomarketplace.customoffer.service.CustomOfferRequestService;
import photomarketplace.customoffer.web.exception.ApiExceptionHandler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomOfferRequestController.class)
@Import(ApiExceptionHandler.class)
class CustomOfferRequestControllerApiTest {

    private static final UUID CUSTOM_OFFER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate EVENT_DATE = LocalDate.of(2030, 8, 15);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 6, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomOfferRequestService customOfferRequestService;

    @Test
    void createShouldReturnCreatedCustomOffer() throws Exception {
        final CreateCustomOfferRequestDTO request = createRequest();

        when(this.customOfferRequestService.create(request))
                .thenReturn(response(CustomOfferStatus.PENDING, null));

        this.mockMvc.perform(post("/api/custom-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(CUSTOM_OFFER_ID.toString()))
                .andExpect(jsonPath("$.clientId").value(CLIENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(this.customOfferRequestService).create(request);
    }

    @Test
    void createShouldReturnValidationErrorsForInvalidRequest() throws Exception {
        final CreateCustomOfferRequestDTO invalidRequest = new CreateCustomOfferRequestDTO(
                null,
                null,
                null,
                LocalDate.now().minusDays(1),
                "",
                "short");

        this.mockMvc.perform(post("/api/custom-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.validationErrors.clientId").exists())
                .andExpect(jsonPath("$.validationErrors.photographerId").exists())
                .andExpect(jsonPath("$.validationErrors.offerId").exists())
                .andExpect(jsonPath("$.validationErrors.eventDate").exists())
                .andExpect(jsonPath("$.validationErrors.location").exists())
                .andExpect(jsonPath("$.validationErrors.message").exists());

        verifyNoInteractions(this.customOfferRequestService);
    }

    @Test
    void createShouldReturnConflictForInvalidOperation() throws Exception {
        final CreateCustomOfferRequestDTO request = createRequest();

        when(this.customOfferRequestService.create(request))
                .thenThrow(new CustomOfferOperationException("A pending request already exists."));

        this.mockMvc.perform(post("/api/custom-offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A pending request already exists."))
                .andExpect(jsonPath("$.path").value("/api/custom-offers"));
    }

    @Test
    void getByIdShouldReturnCustomOffer() throws Exception {
        when(this.customOfferRequestService.getById(CUSTOM_OFFER_ID))
                .thenReturn(response(CustomOfferStatus.PENDING, null));

        this.mockMvc.perform(get("/api/custom-offers/{customOfferId}", CUSTOM_OFFER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOM_OFFER_ID.toString()))
                .andExpect(jsonPath("$.offerId").value(OFFER_ID.toString()));

        verify(this.customOfferRequestService).getById(CUSTOM_OFFER_ID);
    }

    @Test
    void getByIdShouldReturnNotFoundForUnknownCustomOffer() throws Exception {
        when(this.customOfferRequestService.getById(CUSTOM_OFFER_ID))
                .thenThrow(new CustomOfferNotFoundException(CUSTOM_OFFER_ID));

        this.mockMvc.perform(get("/api/custom-offers/{customOfferId}", CUSTOM_OFFER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Custom offer request with id '%s' does not exist.".formatted(CUSTOM_OFFER_ID)));
    }

    @Test
    void getByIdShouldReturnBadRequestForInvalidUuid() throws Exception {
        this.mockMvc.perform(get("/api/custom-offers/{customOfferId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'customOfferId'."));

        verifyNoInteractions(this.customOfferRequestService);
    }

    @Test
    void getForClientShouldReturnClientCustomOffers() throws Exception {
        when(this.customOfferRequestService.getForClient(CLIENT_ID))
                .thenReturn(List.of(response(CustomOfferStatus.PENDING, null)));

        this.mockMvc.perform(get("/api/custom-offers")
                        .param("clientId", CLIENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientId").value(CLIENT_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(this.customOfferRequestService).getForClient(CLIENT_ID);
    }

    @Test
    void getForPhotographerShouldReturnPhotographerCustomOffers() throws Exception {
        when(this.customOfferRequestService.getForPhotographer(PHOTOGRAPHER_ID))
                .thenReturn(List.of(response(CustomOfferStatus.PENDING, null)));

        this.mockMvc.perform(get("/api/custom-offers")
                        .param("photographerId", PHOTOGRAPHER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photographerId").value(PHOTOGRAPHER_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(this.customOfferRequestService).getForPhotographer(PHOTOGRAPHER_ID);
    }

    @Test
    void decideShouldReturnUpdatedCustomOffer() throws Exception {
        final BigDecimal proposedPrice = new BigDecimal("450.00");
        final CustomOfferDecisionRequestDTO request = new CustomOfferDecisionRequestDTO(
                CustomOfferDecisionDTO.ACCEPT,
                proposedPrice);

        when(this.customOfferRequestService.decide(CUSTOM_OFFER_ID, PHOTOGRAPHER_ID, request))
                .thenReturn(response(CustomOfferStatus.ACCEPTED, proposedPrice));

        this.mockMvc.perform(put("/api/custom-offers/{customOfferId}/decision", CUSTOM_OFFER_ID)
                        .param("photographerId", PHOTOGRAPHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.proposedPrice").value(450.00));

        verify(this.customOfferRequestService).decide(CUSTOM_OFFER_ID, PHOTOGRAPHER_ID, request);
    }

    @Test
    void decideShouldReturnValidationErrorWhenDecisionIsMissing() throws Exception {
        final CustomOfferDecisionRequestDTO invalidRequest = new CustomOfferDecisionRequestDTO(null, null);

        this.mockMvc.perform(put("/api/custom-offers/{customOfferId}/decision", CUSTOM_OFFER_ID)
                        .param("photographerId", PHOTOGRAPHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.decision").exists());

        verifyNoInteractions(this.customOfferRequestService);
    }

    @Test
    void withdrawShouldReturnNoContent() throws Exception {
        this.mockMvc.perform(delete("/api/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .param("clientId", CLIENT_ID.toString()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(this.customOfferRequestService).withdraw(CUSTOM_OFFER_ID, CLIENT_ID);
    }

    private static CreateCustomOfferRequestDTO createRequest() {
        return new CreateCustomOfferRequestDTO(
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                OFFER_ID,
                EVENT_DATE,
                "Sofia",
                "Outdoor portrait photography session");
    }

    private static CustomOfferResponseDTO response(final CustomOfferStatus status,
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
                CREATED_AT,
                CREATED_AT);
    }
}
