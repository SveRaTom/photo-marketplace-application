package photomarketplace.service.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import photomarketplace.exception.user.UserRoleManagementException;
import photomarketplace.mapper.user.UserMapper;
import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRoleUpdateDTO;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;
import photomarketplace.repository.user.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;

    public List<UserDTO> getUsers(final UUID administratorId) {
        requireAdministrator(administratorId);

        return this.userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(UserMapper::toUserDTO)
                .toList();
    }

    public void updateUserRole(
            final UUID administratorId,
            final UUID targetUserId,
            final UserRoleUpdateDTO roleUpdate) {

        requireRoleUpdate(roleUpdate);
        requireAdministrator(administratorId);

        if (administratorId.equals(targetUserId)) {
            throw new UserRoleManagementException("Administrators cannot change their own role.");
        }

        final User targetUser = getUser(targetUserId);

        if (targetUser.getRole() == roleUpdate.getRole()) {
            throw new UserRoleManagementException("The user already has the selected role.");
        }

        final UserRole previousRole = targetUser.getRole();
        targetUser.setRole(roleUpdate.getRole());

        this.userRepository.save(targetUser);

        LOGGER.info("Administrator {} changed user {} role from {} to {}.",
                administratorId, targetUserId, previousRole, roleUpdate.getRole());
    }

    private User requireAdministrator(final UUID administratorId) {
        final User administrator = getUser(administratorId);

        if (administrator.getRole() != UserRole.ADMIN) {
            throw new UserRoleManagementException("Only administrators can manage user roles.");
        }

        return administrator;
    }

    private User getUser(final UUID userId) {
        return this.userRepository.findById(userId)
                .orElseThrow(() -> new UserRoleManagementException(
                        "User with id '%s' does not exist.".formatted(userId)));
    }

    private static void requireRoleUpdate(final UserRoleUpdateDTO roleUpdate) {
        if (roleUpdate == null || roleUpdate.getRole() == null) {
            throw new UserRoleManagementException("Select a valid user role.");
        }
    }
}
