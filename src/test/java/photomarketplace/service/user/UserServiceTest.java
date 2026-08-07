package photomarketplace.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import photomarketplace.exception.user.ProfileUpdateException;
import photomarketplace.model.dto.user.ProfileUpdateDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        this.userService = new UserService(this.passwordEncoder, this.userRepository);
    }

    @Test
    void registerShouldNormalizeEmailAndHashPassword() {
        final UserRegisterRequestDTO request = UserRegisterRequestDTO.builder()
                .username("newclient")
                .email("  New.Client@Example.COM  ")
                .password("plainPassword")
                .role(UserRole.CLIENT)
                .build();

        when(this.userRepository.findByEmailIgnoreCase("new.client@example.com")).thenReturn(Optional.empty());
        when(this.passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");

        this.userService.register(request);

        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(this.userRepository).save(userCaptor.capture());

        assertEquals("new.client@example.com", request.getEmail());
        assertEquals("hashedPassword", request.getPassword());
        assertEquals("new.client@example.com", userCaptor.getValue().getEmail());
        assertEquals("hashedPassword", userCaptor.getValue().getPassword());
    }

    @Test
    void registerShouldRejectExistingEmailRegardlessOfCase() {
        final UserRegisterRequestDTO request = UserRegisterRequestDTO.builder()
                .email("Existing@Example.com")
                .password("plainPassword")
                .build();

        when(this.userRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(user(USER_ID, "existing@example.com")));

        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.userService.register(request)
        );

        assertEquals("User with this email already exists!", exception.getMessage());

        verifyNoInteractions(this.passwordEncoder);
        verify(this.userRepository, never()).save(any(User.class));
    }

    @Test
    void getProfileForEditShouldMapEditableFields() {
        final User user = user(USER_ID, "client@example.com");
        user.setFirstName("Alex");
        user.setLastName("Morgan");
        user.setProfileImageUrl("https://example.com/avatar.jpg");

        when(this.userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        final ProfileUpdateDTO profile = this.userService.getProfileForEdit(USER_ID);

        assertEquals("Alex", profile.getFirstName());
        assertEquals("Morgan", profile.getLastName());
        assertEquals("client@example.com", profile.getEmail());
        assertEquals("https://example.com/avatar.jpg", profile.getProfileImageUrl());
    }

    @Test
    void updateProfileShouldNormalizeAndPersistEditableFields() {
        final User user = user(USER_ID, "old@example.com");
        final ProfileUpdateDTO profile = ProfileUpdateDTO.builder()
                .firstName("  Alex  ")
                .lastName("  Morgan  ")
                .email("  Updated@Example.COM  ")
                .profileImageUrl("   ")
                .build();

        when(this.userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(this.userRepository.findByEmailIgnoreCase("updated@example.com"))
                .thenReturn(Optional.of(user));

        this.userService.updateProfile(USER_ID, profile);

        assertEquals("Alex", user.getFirstName());
        assertEquals("Morgan", user.getLastName());
        assertEquals("updated@example.com", user.getEmail());
        assertNull(user.getProfileImageUrl());

        verify(this.userRepository).save(user);
    }

    @Test
    void updateProfileShouldRejectEmailOwnedByAnotherUser() {
        final User user = user(USER_ID, "client@example.com");
        final User otherUser = user(OTHER_USER_ID, "taken@example.com");
        final ProfileUpdateDTO profile = ProfileUpdateDTO.builder()
                .firstName("Alex")
                .lastName("Morgan")
                .email("TAKEN@example.com")
                .build();

        when(this.userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(this.userRepository.findByEmailIgnoreCase("taken@example.com"))
                .thenReturn(Optional.of(otherUser));

        final ProfileUpdateException exception = assertThrows(
                ProfileUpdateException.class,
                () -> this.userService.updateProfile(USER_ID, profile)
        );

        assertEquals("An account with this email already exists.", exception.getMessage());
        assertEquals("client@example.com", user.getEmail());

        verify(this.userRepository, never()).save(user);
    }

    @Test
    void updateProfileShouldRejectIncompleteDetailsBeforeRepositoryAccess() {
        final ProfileUpdateDTO profile = ProfileUpdateDTO.builder()
                .firstName(" ")
                .lastName("Morgan")
                .email("client@example.com")
                .build();

        final ProfileUpdateException exception = assertThrows(
                ProfileUpdateException.class,
                () -> this.userService.updateProfile(USER_ID, profile)
        );

        assertEquals("Complete profile details are required.", exception.getMessage());

        verifyNoInteractions(this.userRepository);
    }

    private static User user(final UUID id, final String email) {
        return User.builder()
                .id(id)
                .username("marketplace-user")
                .email(email)
                .password("hashedPassword")
                .role(UserRole.CLIENT)
                .isActive(true)
                .build();
    }
}
