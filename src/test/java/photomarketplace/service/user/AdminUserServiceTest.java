package photomarketplace.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import photomarketplace.exception.user.UserRoleManagementException;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRoleUpdateDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.user.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final UUID ADMINISTRATOR_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private UserRepository userRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        this.adminUserService = new AdminUserService(this.userRepository);
    }

    @Test
    void getUsersShouldRequireAdministratorAndSortUsersByEmail() {
        final User administrator = user(ADMINISTRATOR_ID, "admin@example.com", UserRole.ADMIN);
        final User firstUser = user(TARGET_USER_ID, "alpha@example.com", UserRole.CLIENT);
        final User secondUser = user(UUID.randomUUID(), "Zulu@example.com", UserRole.PHOTOGRAPHER);

        when(this.userRepository.findById(ADMINISTRATOR_ID)).thenReturn(Optional.of(administrator));
        when(this.userRepository.findAll()).thenReturn(List.of(secondUser, firstUser, administrator));

        final List<UserDTO> users = this.adminUserService.getUsers(ADMINISTRATOR_ID);

        assertEquals(List.of("admin@example.com", "alpha@example.com", "Zulu@example.com"),
                users.stream().map(UserDTO::getEmail).toList());
    }

    @Test
    void updateUserRoleShouldPersistSelectedRole() {
        final User administrator = user(ADMINISTRATOR_ID, "admin@example.com", UserRole.ADMIN);
        final User targetUser = user(TARGET_USER_ID, "client@example.com", UserRole.CLIENT);
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder()
                .role(UserRole.PHOTOGRAPHER)
                .build();

        when(this.userRepository.findById(ADMINISTRATOR_ID)).thenReturn(Optional.of(administrator));
        when(this.userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

        this.adminUserService.updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update);

        assertEquals(UserRole.PHOTOGRAPHER, targetUser.getRole());

        verify(this.userRepository).save(targetUser);
    }

    @Test
    void updateUserRoleShouldRejectNonAdministrator() {
        final User client = user(ADMINISTRATOR_ID, "client@example.com", UserRole.CLIENT);
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder()
                .role(UserRole.PHOTOGRAPHER)
                .build();

        when(this.userRepository.findById(ADMINISTRATOR_ID)).thenReturn(Optional.of(client));

        final UserRoleManagementException exception = assertThrows(
                UserRoleManagementException.class,
                () -> this.adminUserService.updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update)
        );

        assertEquals("Only administrators can manage user roles.", exception.getMessage());

        verify(this.userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoleShouldRejectAdministratorChangingOwnRole() {
        final User administrator = user(ADMINISTRATOR_ID, "admin@example.com", UserRole.ADMIN);
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder()
                .role(UserRole.CLIENT)
                .build();

        when(this.userRepository.findById(ADMINISTRATOR_ID)).thenReturn(Optional.of(administrator));

        final UserRoleManagementException exception = assertThrows(
                UserRoleManagementException.class,
                () -> this.adminUserService.updateUserRole(ADMINISTRATOR_ID, ADMINISTRATOR_ID, update)
        );

        assertEquals("Administrators cannot change their own role.", exception.getMessage());

        verify(this.userRepository, never()).save(administrator);
    }

    @Test
    void updateUserRoleShouldRejectUnchangedRole() {
        final User administrator = user(ADMINISTRATOR_ID, "admin@example.com", UserRole.ADMIN);
        final User targetUser = targetUser();
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder()
                .role(UserRole.CLIENT)
                .build();

        when(this.userRepository.findById(ADMINISTRATOR_ID)).thenReturn(Optional.of(administrator));
        when(this.userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

        final UserRoleManagementException exception = assertThrows(
                UserRoleManagementException.class,
                () -> this.adminUserService.updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update)
        );

        assertEquals("The user already has the selected role.", exception.getMessage());

        verify(this.userRepository, never()).save(targetUser);
    }

    @Test
    void updateUserRoleShouldRejectMissingRoleBeforeRepositoryAccess() {
        final UserRoleUpdateDTO update = UserRoleUpdateDTO.builder().build();

        final UserRoleManagementException exception = assertThrows(
                UserRoleManagementException.class,
                () -> this.adminUserService.updateUserRole(ADMINISTRATOR_ID, TARGET_USER_ID, update)
        );

        assertEquals("Select a valid user role.", exception.getMessage());

        verifyNoInteractions(this.userRepository);
    }

    @Test
    void getUsersShouldRejectMissingAdministrator() {
        when(this.userRepository.findById(ADMINISTRATOR_ID)).thenReturn(Optional.empty());

        final UserRoleManagementException exception = assertThrows(
                UserRoleManagementException.class,
                () -> this.adminUserService.getUsers(ADMINISTRATOR_ID)
        );

        assertEquals("User with id '%s' does not exist.".formatted(ADMINISTRATOR_ID),
                exception.getMessage());
    }

    private static User targetUser() {
        return user(TARGET_USER_ID, "client@example.com", UserRole.CLIENT);
    }

    private static User user(final UUID id, final String email, final UserRole role) {
        return User.builder()
                .id(id)
                .username(email.substring(0, email.indexOf('@')))
                .email(email)
                .password("hashedPassword")
                .role(role)
                .isActive(true)
                .build();
    }
}
