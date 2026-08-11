package photomarketplace.web.offer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import photomarketplace.model.dto.offer.OfferDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.service.offer.OfferService;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class OfferControllerTest {

    private static final UUID OFFER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CUSTOM_OFFER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLIENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OfferService offerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new OfferController(this.offerService)).build();
    }

    @Test
    void getOfferDetailsShouldReturnToExactCustomOfferRequest() throws Exception {
        final OfferDTO offer = offer();

        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer);

        this.mockMvc.perform(get("/offers/{id}", OFFER_ID)
                        .param("from", "photographer-custom-offers")
                        .param("customOfferId", CUSTOM_OFFER_ID.toString())
                        .session(photographerSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("offer-details"))
                .andExpect(model().attribute("offer", offer))
                .andExpect(model().attribute(
                        "backUrl",
                        "/photographer/custom-offers#custom-offer-" + CUSTOM_OFFER_ID));
    }

    @Test
    void getOfferDetailsShouldKeepDefaultPhotographerBackTarget() throws Exception {
        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer());

        this.mockMvc.perform(get("/offers/{id}", OFFER_ID).session(photographerSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("backUrl", "/my-offers"));
    }

    @Test
    void getOfferDetailsShouldReturnClientToExactCustomOfferRequest() throws Exception {
        when(this.offerService.getOfferById(OFFER_ID)).thenReturn(offer());

        this.mockMvc.perform(get("/offers/{id}", OFFER_ID)
                        .param("from", "client-custom-offers")
                        .param("customOfferId", CUSTOM_OFFER_ID.toString())
                        .session(clientSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("offer-details"))
                .andExpect(model().attribute(
                        "backUrl",
                        "/custom-offers#custom-offer-" + CUSTOM_OFFER_ID));
    }

    private static OfferDTO offer() {
        return OfferDTO.builder()
                .id(OFFER_ID)
                .photographer(UserDTO.builder().id(PHOTOGRAPHER_ID).build())
                .build();
    }

    private static MockHttpSession photographerSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", PHOTOGRAPHER_ID);

        return session;
    }

    private static MockHttpSession clientSession() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", CLIENT_ID);

        return session;
    }
}
