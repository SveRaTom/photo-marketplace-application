package photomarketplace.security;

import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    private static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Set<String> UNAUTHENTICATED_ENDPOINTS = Set.of("/", "/login", "/register", "/error");
    private static final Set<String> PUBLIC_GET_ENDPOINTS = Set.of("/offers", "/offers/{id}", "/photographers/{id}");
    private static final Set<String> PHOTOGRAPHER_ENDPOINTS = Set.of("/my-offers", "/offers/create",
            "/offers/edit/{id}", "/offers/delete/{id}", "/portfolio/{id}", "/dashboard");

    private final UserService userService;

    @Autowired
    public SessionInterceptor(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {

        final String requestURI = request.getRequestURI();

        if (UNAUTHENTICATED_ENDPOINTS.contains(requestURI) || isPublicGetRequest(request)) {
            return true;
        }

        final HttpSession session = request.getSession(false);

        if (session == null) {
            return loginRedirect(response);
        }

        final UUID userId = (UUID) session.getAttribute("user_id");

        if (userId == null) {
            session.invalidate();
            return loginRedirect(response);
        }

        final UserDTO user = this.userService.getUserById(userId);

        if (user == null || !user.isActive()) {
            session.invalidate();
            return loginRedirect(response);
        }

        if (isPhotographerEndpoint(request) && user.getRole() != UserRole.PHOTOGRAPHER) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("You do not have permission to access this resource.");
            return false;
        }

        return true;
    }

    private static boolean isPublicGetRequest(final HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod()) && matchesAnyPattern(request, PUBLIC_GET_ENDPOINTS);
    }

    private static boolean isPhotographerEndpoint(final HttpServletRequest request) {
        return matchesAnyPattern(request, PHOTOGRAPHER_ENDPOINTS);
    }

    private static boolean matchesAnyPattern(final HttpServletRequest request, final Iterable<String> patterns) {
        final String requestURI = request.getRequestURI();

        for (final String pattern : patterns) {
            if (requestURI.matches(pattern.replace("{id}", UUID_REGEX).replace("*", "[^/]+"))) {
                return true;
            }
        }

        return false;
    }

    private static boolean loginRedirect(final HttpServletResponse response) throws IOException {
        response.sendRedirect("/login");
        return false;
    }
}
