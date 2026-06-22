package photomarketplace.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import photomarketplace.model.entity.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserRegisterRequestDTO {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 6, message = "Username must be at least 6 characters")
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "User role must not be null")
    private UserRole role;
}
