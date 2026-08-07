package photomarketplace.model.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import photomarketplace.model.entity.user.UserRole;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleUpdateDTO {

    @NotNull(message = "Select a user role")
    private UserRole role;
}
