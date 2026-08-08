package photomarketplace.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MarketplaceAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    MarketplaceAuthenticationSuccessHandler() {
        setDefaultTargetUrl("/home");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication) throws IOException, ServletException {

        final MarketplaceUserDetails userDetails = (MarketplaceUserDetails) authentication.getPrincipal();
        request.getSession().setAttribute(MarketplaceSession.USER_ID, userDetails.getId());

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
