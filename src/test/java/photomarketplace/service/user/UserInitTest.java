package photomarketplace.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.entity.user.UserRole;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInitTest {

    private static final String PHOTOGRAPHER_PASSWORD = "photographer-pass";
    private static final String CLIENT_PASSWORD = "client-pass";
    private static final String ADMINISTRATOR_PASSWORD = "administrator-pass";

    @Mock
    private UserService userService;

    private UserInit userInit;

    @BeforeEach
    void setUp() {
        this.userInit = new UserInit(this.userService);
        setPasswords(PHOTOGRAPHER_PASSWORD, CLIENT_PASSWORD, ADMINISTRATOR_PASSWORD);
    }

    @Test
    void runShouldSeedAccountsWithConfiguredPasswords() {
        when(this.userService.getAllUsers()).thenReturn(List.of());

        this.userInit.run();

        verify(this.userService).registerInitialUser(argThat(request ->
                request.getRole() == UserRole.PHOTOGRAPHER
                        && "photographer".equals(request.getUsername())
                        && "photographer@example.com".equals(request.getEmail())
                        && PHOTOGRAPHER_PASSWORD.equals(request.getPassword())));
        verify(this.userService).registerInitialUser(argThat(request ->
                request.getRole() == UserRole.CLIENT
                        && "client".equals(request.getUsername())
                        && "client@example.com".equals(request.getEmail())
                        && CLIENT_PASSWORD.equals(request.getPassword())));
        verify(this.userService).registerInitialUser(argThat(request ->
                request.getRole() == UserRole.ADMIN
                        && "administrator".equals(request.getUsername())
                        && "admin@example.com".equals(request.getEmail())
                        && ADMINISTRATOR_PASSWORD.equals(request.getPassword())));
    }

    @Test
    void runShouldSkipAccountsWithoutConfiguredPasswords() {
        setPasswords("", " ", null);

        when(this.userService.getAllUsers()).thenReturn(List.of());

        this.userInit.run();

        verify(this.userService).getAllUsers();
        verifyNoMoreInteractions(this.userService);
    }

    @Test
    void runShouldNotRecreateExistingAccount() {
        final UserDTO existingPhotographer = UserDTO.builder()
                .email("PHOTOGRAPHER@EXAMPLE.COM")
                .build();

        when(this.userService.getAllUsers()).thenReturn(List.of(existingPhotographer));

        this.userInit.run();

        verify(this.userService, never()).registerInitialUser(argThat(request ->
                request.getRole() == UserRole.PHOTOGRAPHER));
        verify(this.userService).registerInitialUser(argThat(request ->
                request.getRole() == UserRole.CLIENT));
        verify(this.userService).registerInitialUser(argThat(request ->
                request.getRole() == UserRole.ADMIN));
    }

    private void setPasswords(final String photographerPassword,
                              final String clientPassword,
                              final String administratorPassword) {

        ReflectionTestUtils.setField(this.userInit, "photographerPassword", photographerPassword);
        ReflectionTestUtils.setField(this.userInit, "clientPassword", clientPassword);
        ReflectionTestUtils.setField(this.userInit, "administratorPassword", administratorPassword);
    }
}
