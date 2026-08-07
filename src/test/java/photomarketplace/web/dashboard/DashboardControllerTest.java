package photomarketplace.web.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;
import photomarketplace.model.dto.dashboard.PhotographerDashboardDTO;
import photomarketplace.service.dashboard.DashboardService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private static final UUID PHOTOGRAPHER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private DashboardService dashboardService;

    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        this.dashboardController = new DashboardController(this.dashboardService);
    }

    @Test
    void getDashboardShouldReturnDashboardViewModel() throws Exception {
        final PhotographerDashboardDTO dashboard = PhotographerDashboardDTO.builder()
                .averageRating(new BigDecimal("0.0"))
                .upcomingBookings(List.of())
                .recentReviews(List.of())
                .build();

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute("user_id", PHOTOGRAPHER_ID);

        when(this.dashboardService.getPhotographerDashboard(PHOTOGRAPHER_ID)).thenReturn(dashboard);

        final ModelAndView modelAndView = this.dashboardController.getDashboard(session);

        assertEquals("dashboard", modelAndView.getViewName());
        assertSame(dashboard, modelAndView.getModel().get("dashboard"));

        verify(this.dashboardService).getPhotographerDashboard(PHOTOGRAPHER_ID);
    }
}
