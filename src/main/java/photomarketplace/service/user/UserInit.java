package photomarketplace.service.user;

import org.springframework.beans.factory.annotation.Value;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserInit implements CommandLineRunner {

    @Value("${app.photographer.password}")
    private String photographerPassword;

    @Value("${app.client.password}")
    private String clientPassword;

    private final UserService userService;

    public UserInit(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(final String... args) {
        final List<UserDTO> users = this.userService.getAllUsers();

        if (!users.isEmpty()) {
            return;
        }

        final UserRegisterRequestDTO photographerRegisterRequest = UserRegisterRequestDTO.builder()
                .username("photographer")
                .email("photographer@example.com")
                .password(this.photographerPassword)
                .role(UserRole.PHOTOGRAPHER)
                .build();

        this.userService.register(photographerRegisterRequest);

        log.info("Photographer account created with username '%s'.".formatted(photographerRegisterRequest.getUsername()));

        final UserRegisterRequestDTO clientRegisterRequest = UserRegisterRequestDTO.builder()
                .username("client")
                .email("client@example.com")
                .password(this.clientPassword)
                .role(UserRole.CLIENT)
                .build();

        this.userService.register(clientRegisterRequest);

        log.info("Client account created with username '%s'.".formatted(clientRegisterRequest.getUsername()));
    }
}
