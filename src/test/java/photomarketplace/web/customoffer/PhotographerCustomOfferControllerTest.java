package photomarketplace.web.customoffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import photomarketplace.exception.customoffer.CustomOfferOperationException;
import photomarketplace.exception.customoffer.CustomOfferServiceUnavailableException;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionDTO;
import photomarketplace.model.dto.customoffer.CustomOfferDecisionRequestDTO;
import photomarketplace.model.dto.customoffer.CustomOfferResponseDTO;
import photomarketplace.model.dto.customoffer.CustomOfferStatusDTO;
import photomarketplace.service.customoffer.CustomOfferService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class PhotographerCustomOfferControllerTest {

    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CUSTOM_OFFER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private CustomOfferService customOfferService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.validator = new LocalValidatorFactoryBean();
        this.validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                        new PhotographerCustomOfferController(this.customOfferService))
                .setValidator(this.validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        this.validator.close();
    }

    @Test
    void getPhotographerCustomOffersShouldReturnPopulatedPage() throws Exception {
        final List<CustomOfferResponseDTO> customOffers = List.of(customOffer());

        when(this.customOfferService.getPhotographerCustomOffers(PHOTOGRAPHER_ID)).thenReturn(customOffers);

        this.mockMvc.perform(get("/photographer/custom-offers").session(photographerSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("photographer-custom-offers"))
                .andExpect(model().attribute("customOffers", customOffers));
    }

    @Test
    void getPhotographerCustomOffersShouldShowServiceErrorAndEmptyList() throws Exception {
        when(this.customOfferService.getPhotographerCustomOffers(PHOTOGRAPHER_ID))
                .thenThrow(new CustomOfferServiceUnavailableException("Custom offers are temporarily unavailable."));

        this.mockMvc.perform(get("/photographer/custom-offers").session(photographerSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("photographer-custom-offers"))
                .andExpect(model().attribute("customOffers", List.of()))
                .andExpect(model().attribute("errorMessage", "Custom offers are temporarily unavailable."));
    }

    @Test
    void acceptCustomOfferShouldDelegateAndRedirectWithConfirmation() throws Exception {
        this.mockMvc.perform(put("/photographer/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .session(photographerSession())
                        .param("decision", "ACCEPT")
                        .param("proposedPrice", "450.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/photographer/custom-offers"))
                .andExpect(flash().attribute("successMessage", "The custom offer was accepted successfully."));

        final ArgumentCaptor<CustomOfferDecisionRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(CustomOfferDecisionRequestDTO.class);

        verify(this.customOfferService).decideCustomOffer(
                eq(CUSTOM_OFFER_ID),
                eq(PHOTOGRAPHER_ID),
                requestCaptor.capture());

        assertEquals(CustomOfferDecisionDTO.ACCEPT, requestCaptor.getValue().decision());
        assertEquals(new BigDecimal("450.00"), requestCaptor.getValue().proposedPrice());
    }

    @Test
    void declineCustomOfferShouldDelegateWithoutPriceAndRedirectWithConfirmation() throws Exception {
        this.mockMvc.perform(put("/photographer/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .session(photographerSession())
                        .param("decision", "DECLINE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/photographer/custom-offers"))
                .andExpect(flash().attribute("successMessage", "The custom offer was declined successfully."));

        final ArgumentCaptor<CustomOfferDecisionRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(CustomOfferDecisionRequestDTO.class);

        verify(this.customOfferService).decideCustomOffer(
                eq(CUSTOM_OFFER_ID),
                eq(PHOTOGRAPHER_ID),
                requestCaptor.capture());

        assertEquals(CustomOfferDecisionDTO.DECLINE, requestCaptor.getValue().decision());
        assertNull(requestCaptor.getValue().proposedPrice());
    }

    @Test
    void acceptCustomOfferShouldRejectInvalidPrice() throws Exception {
        this.mockMvc.perform(put("/photographer/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .session(photographerSession())
                        .param("decision", "ACCEPT")
                        .param("proposedPrice", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/photographer/custom-offers"))
                .andExpect(flash().attribute("errorMessage", "The proposed price must be positive"));

        verify(this.customOfferService, never()).decideCustomOffer(any(), any(), any());
    }

    @Test
    void decideCustomOfferShouldRedirectWithServiceError() throws Exception {
        final String errorMessage = "Only pending custom offers can be accepted or declined.";

        when(this.customOfferService.decideCustomOffer(
                eq(CUSTOM_OFFER_ID),
                eq(PHOTOGRAPHER_ID),
                any(CustomOfferDecisionRequestDTO.class)))
                .thenThrow(new CustomOfferOperationException(errorMessage));

        this.mockMvc.perform(put("/photographer/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .session(photographerSession())
                        .param("decision", "ACCEPT")
                        .param("proposedPrice", "450.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/photographer/custom-offers"))
                .andExpect(flash().attribute("errorMessage", errorMessage));
    }

    private static MockHttpSession photographerSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", PHOTOGRAPHER_ID);

        return session;
    }

    private static CustomOfferResponseDTO customOffer() {
        return new CustomOfferResponseDTO(
                CUSTOM_OFFER_ID,
                CLIENT_ID,
                PHOTOGRAPHER_ID,
                OFFER_ID,
                LocalDate.of(2030, 8, 15),
                "Sofia",
                "Outdoor portrait photography session",
                null,
                CustomOfferStatusDTO.PENDING,
                LocalDateTime.of(2026, 8, 7, 10, 30),
                LocalDateTime.of(2026, 8, 7, 10, 30));
    }
}
