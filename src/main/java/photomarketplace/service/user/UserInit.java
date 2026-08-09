package photomarketplace.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.UserRole;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInit implements CommandLineRunner {

    @Value("${app.photographer.password}")
    private String photographerPassword;

    @Value("${app.client.password}")
    private String clientPassword;

    @Value("${app.admin.password}")
    private String administratorPassword;

    private final UserService userService;

    @Override
    public void run(final String... args) {
        final List<UserDTO> users = this.userService.getAllUsers();

        createIfMissing(
                users,
                "photographer",
                "Alex",
                "Morgan",
                "photographer@example.com",
                this.photographerPassword,
                UserRole.PHOTOGRAPHER
        );
        createIfMissing(
                users,
                "client",
                "Emma",
                "Carter",
                "client@example.com",
                this.clientPassword,
                UserRole.CLIENT
        );
        createIfMissing(
                users,
                "administrator",
                "System",
                "Administrator",
                "admin@example.com",
                this.administratorPassword,
                UserRole.ADMIN
        );
    }

    private void createIfMissing(
            final List<UserDTO> users,
            final String username,
            final String firstName,
            final String lastName,
            final String email,
            final String password,
            final UserRole role) {

        final boolean accountExists = users.stream()
                .anyMatch(user -> email.equalsIgnoreCase(user.getEmail()));

        if (accountExists) {
            return;
        }

        if (password == null || password.isBlank()) {
            log.warn("{} account was not seeded because its password is not configured.", role);
            return;
        }

        final UserRegisterRequestDTO registration = UserRegisterRequestDTO.builder()
                .username(username)
                .email(email)
                .password(password)
                .role(role)
                .build();

        this.userService.registerInitialUser(registration, firstName, lastName);

        log.info("{} account created with username '{}'.", role, username);
    }
}
