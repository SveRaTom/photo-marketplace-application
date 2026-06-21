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

    private static final Set<String> UNAUTHENTICATED_ENDPOINTS = Set.of("/", "/login", "/register", "/error");
    private static final Set<String> PHOTOGRAPHER_ENDPOINTS = Set.of("/offers/create", "/offers/edit", "/offers/delete");

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

        if (UNAUTHENTICATED_ENDPOINTS.contains(requestURI)) {
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

        if (PHOTOGRAPHER_ENDPOINTS.contains(requestURI) && user.getRole() != UserRole.PHOTOGRAPHER) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("You do not have permission to access this resource.");
            return false;
        }

        return true;
    }

    private static boolean loginRedirect(final HttpServletResponse response) throws IOException {
        response.sendRedirect("/login");
        return false;
    }
}
