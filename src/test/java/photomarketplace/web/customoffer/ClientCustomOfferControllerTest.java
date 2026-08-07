package photomarketplace.web.customoffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import photomarketplace.exception.customoffer.CustomOfferOperationException;
import photomarketplace.exception.customoffer.CustomOfferServiceUnavailableException;
import photomarketplace.model.dto.customoffer.CustomOfferResponseDTO;
import photomarketplace.model.dto.customoffer.CustomOfferStatusDTO;
import photomarketplace.service.customoffer.CustomOfferService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ClientCustomOfferControllerTest {

    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CUSTOM_OFFER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private CustomOfferService customOfferService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                        new ClientCustomOfferController(this.customOfferService))
                .build();
    }

    @Test
    void getClientCustomOffersShouldReturnPopulatedPage() throws Exception {
        final List<CustomOfferResponseDTO> customOffers = List.of(customOffer());

        when(this.customOfferService.getClientCustomOffers(CLIENT_ID)).thenReturn(customOffers);

        this.mockMvc.perform(get("/custom-offers").session(clientSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("my-custom-offers"))
                .andExpect(model().attribute("customOffers", customOffers));
    }

    @Test
    void getClientCustomOffersShouldShowServiceErrorAndEmptyList() throws Exception {
        when(this.customOfferService.getClientCustomOffers(CLIENT_ID))
                .thenThrow(new CustomOfferServiceUnavailableException("Custom offers are temporarily unavailable."));

        this.mockMvc.perform(get("/custom-offers").session(clientSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("my-custom-offers"))
                .andExpect(model().attribute("customOffers", List.of()))
                .andExpect(model().attribute("errorMessage", "Custom offers are temporarily unavailable."));
    }

    @Test
    void withdrawCustomOfferShouldDelegateAndRedirectWithConfirmation() throws Exception {
        this.mockMvc.perform(delete("/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .session(clientSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-offers"))
                .andExpect(flash().attribute("successMessage",
                        "Your custom offer request was withdrawn successfully."));

        verify(this.customOfferService).withdrawCustomOffer(CUSTOM_OFFER_ID, CLIENT_ID);
    }

    @Test
    void withdrawCustomOfferShouldRedirectWithServiceError() throws Exception {
        final String errorMessage = "Only pending custom offers can be withdrawn.";

        doThrow(new CustomOfferOperationException(errorMessage))
                .when(this.customOfferService)
                .withdrawCustomOffer(CUSTOM_OFFER_ID, CLIENT_ID);

        this.mockMvc.perform(delete("/custom-offers/{customOfferId}", CUSTOM_OFFER_ID)
                        .session(clientSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-offers"))
                .andExpect(flash().attribute("errorMessage", errorMessage));
    }

    private static MockHttpSession clientSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", CLIENT_ID);

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
                new BigDecimal("450.00"),
                CustomOfferStatusDTO.PENDING,
                LocalDateTime.of(2026, 8, 7, 10, 30),
                LocalDateTime.of(2026, 8, 7, 10, 30));
    }
}
