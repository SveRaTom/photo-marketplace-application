package photomarketplace.model.dto.user;

import lombok.Builder;
import lombok.Data;
import photomarketplace.model.entity.user.UserRole;

import java.util.UUID;

@Builder
@Data
public class UserDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String displayName;
    private String username;
    private String email;
    private UserRole role;
    private boolean isActive;
}
