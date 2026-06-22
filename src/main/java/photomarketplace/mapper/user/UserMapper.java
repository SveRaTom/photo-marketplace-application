package photomarketplace.mapper.user;

import photomarketplace.model.dto.user.UserDTO;
import photomarketplace.model.dto.user.UserRegisterRequestDTO;
import photomarketplace.model.entity.user.User;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserMapper {

    public static User toUserEntity(final UserRegisterRequestDTO userRegisterRequest) {
        if (userRegisterRequest == null) {
            return null;
        }

        return User.builder()
                .username(userRegisterRequest.getUsername())
                .email(userRegisterRequest.getEmail())
                .password(userRegisterRequest.getPassword())
                .role(userRegisterRequest.getRole())
                .isActive(true)
                .build();
    }

    public static UserDTO toUserDTO(final User user) {
        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(displayName(user))
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.isActive())
                .role(user.getRole())
                .build();
    }

    private static String displayName(final User user) {
        final String fullName = "%s %s".formatted(
                user.getFirstName() == null ? "" : user.getFirstName(),
                user.getLastName() == null ? "" : user.getLastName()
        ).trim();

        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
