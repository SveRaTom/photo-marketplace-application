package photomarketplace.service.user;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import photomarketplace.config.cache.EvictOfferCaches;
import photomarketplace.exception.ResourceNotFoundException;
import photomarketplace.exception.user.ProfileUpdateException;
import photomarketplace.exception.user.UserRegistrationException;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.user.ProfileUpdateDTO;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.user.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    public UserService(final PasswordEncoder passwordEncoder, final UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public void register(final UserRegisterRequestDTO userRegisterRequest) {
        if (userRegisterRequest.getRole() != UserRole.CLIENT
                && userRegisterRequest.getRole() != UserRole.PHOTOGRAPHER) {

            throw new UserRegistrationException(
                    "Only client or photographer accounts can be created through registration.");
        }

        createUser(userRegisterRequest);
    }

    void registerInitialUser(final UserRegisterRequestDTO userRegisterRequest) {
        createUser(userRegisterRequest);
    }

    private void createUser(final UserRegisterRequestDTO userRegisterRequest) {
        final String normalizedEmail = normalizeEmail(userRegisterRequest.getEmail());

        this.userRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(user -> {
                    throw new UserRegistrationException("User with this email already exists!");
                });

        final String encodedPassword = this.passwordEncoder.encode(userRegisterRequest.getPassword());
        userRegisterRequest.setEmail(normalizedEmail);
        userRegisterRequest.setPassword(encodedPassword);

        final User userEntity = UserMapper.toUserEntity(userRegisterRequest);

        this.userRepository.save(userEntity);
    }

    public UserDTO getUserById(final UUID id) {
        return UserMapper.toUserDTO(getUser(id));
    }

    public ProfileUpdateDTO getProfileForEdit(final UUID userId) {
        final User user = getUser(userId);

        return ProfileUpdateDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    @EvictOfferCaches
    public void updateProfile(final UUID userId, final ProfileUpdateDTO profileUpdate) {
        requireProfileDetails(profileUpdate);

        final User user = getUser(userId);
        final String normalizedEmail = normalizeEmail(profileUpdate.getEmail());

        this.userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new ProfileUpdateException("An account with this email already exists.");
                });

        user.setFirstName(profileUpdate.getFirstName().trim());
        user.setLastName(profileUpdate.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setProfileImageUrl(normalizeOptional(profileUpdate.getProfileImageUrl()));

        this.userRepository.save(user);
        LOGGER.info("User {} updated their profile.", userId);
    }

    public User getUser(final UUID id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public List<UserDTO> getAllUsers() {
        return this.userRepository.findAll().stream()
                .map(UserMapper::toUserDTO)
                .toList();
    }

    private static void requireProfileDetails(final ProfileUpdateDTO profileUpdate) {
        if (profileUpdate == null
                || profileUpdate.getFirstName() == null
                || profileUpdate.getFirstName().isBlank()
                || profileUpdate.getLastName() == null
                || profileUpdate.getLastName().isBlank()
                || profileUpdate.getEmail() == null
                || profileUpdate.getEmail().isBlank()) {

            throw new ProfileUpdateException("Complete profile details are required.");
        }
    }

    private static String normalizeOptional(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
