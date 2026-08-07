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
import photomarketplace.model.dto.customoffer.CustomOfferRequestDTO;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.service.customoffer.CustomOfferService;
import photomarketplace.service.offer.OfferService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class CustomOfferControllerTest {

    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private CustomOfferService customOfferService;

    @Mock
    private OfferService offerService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.validator = new LocalValidatorFactoryBean();
        this.validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                        new CustomOfferController(this.customOfferService, this.offerService))
                .setValidator(this.validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        this.validator.close();
    }

    @Test
    void getCreatePageShouldReturnPopulatedForm() throws Exception {
        final OfferDTO offer = offer();

        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer);

        this.mockMvc.perform(get("/offers/{offerId}/custom-offer", OFFER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("create-custom-offer"))
                .andExpect(model().attribute("offer", offer))
                .andExpect(model().attribute("offerId", OFFER_ID))
                .andExpect(model().attributeExists("minimumEventDate"))
                .andExpect(model().attributeExists("customOfferRequestDTO"));
    }

    @Test
    void createShouldDelegateAndRedirectWithConfirmation() throws Exception {
        final MockHttpSession session = clientSession();

        this.mockMvc.perform(post("/offers/{offerId}/custom-offer", OFFER_ID)
                        .session(session)
                        .param("eventDate", "2030-08-15")
                        .param("location", "Sofia")
                        .param("message", "Outdoor portrait photography session"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/offers/" + OFFER_ID))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Your custom offer request was sent successfully."));

        final ArgumentCaptor<CustomOfferRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(CustomOfferRequestDTO.class);

        verify(this.customOfferService).createCustomOffer(eq(OFFER_ID), eq(CLIENT_ID), requestCaptor.capture());

        final CustomOfferRequestDTO request = requestCaptor.getValue();

        assertEquals(LocalDate.of(2030, 8, 15), request.getEventDate());
        assertEquals("Sofia", request.getLocation());
        assertEquals("Outdoor portrait photography session", request.getMessage());
    }

    @Test
    void createShouldReturnFormWithValidationErrors() throws Exception {
        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer());

        this.mockMvc.perform(post("/offers/{offerId}/custom-offer", OFFER_ID)
                        .session(clientSession())
                        .param("eventDate", "2020-01-01")
                        .param("location", "")
                        .param("message", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-custom-offer"))
                .andExpect(model().attributeHasFieldErrors(
                        "customOfferRequestDTO",
                        "eventDate",
                        "location",
                        "message"));

        verify(this.customOfferService, never()).createCustomOffer(
                any(UUID.class),
                any(UUID.class),
                any(CustomOfferRequestDTO.class));
    }

    @Test
    void createShouldShowServiceErrorOnForm() throws Exception {
        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer());
        when(this.customOfferService.createCustomOffer(
                eq(OFFER_ID),
                eq(CLIENT_ID),
                any(CustomOfferRequestDTO.class)))
                .thenThrow(new CustomOfferOperationException("A pending request already exists."));

        this.mockMvc.perform(post("/offers/{offerId}/custom-offer", OFFER_ID)
                        .session(clientSession())
                        .param("eventDate", "2030-08-15")
                        .param("location", "Sofia")
                        .param("message", "Outdoor portrait photography session"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-custom-offer"))
                .andExpect(model().attribute("formError", "A pending request already exists."));
    }

    private static MockHttpSession clientSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", CLIENT_ID);

        return session;
    }

    private static OfferDTO offer() {
        return OfferDTO.builder()
                .id(OFFER_ID)
                .title("Wedding Photography")
                .price(new BigDecimal("500.00"))
                .durationHours(6)
                .isAvailable(true)
                .photographer(UserDTO.builder()
                        .id(PHOTOGRAPHER_ID)
                        .displayName("Studio Photographer")
                        .build())
                .build();
    }
}
