package photomarketplace.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import photomarketplace.model.entity.user.User;
import photomarketplace.model.entity.user.UserRole;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MarketplaceUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final boolean active;

    MarketplaceUserDetails(final User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    UUID getId() {
        return this.id;
    }

    UserRole getRole() {
        return this.role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.active;
    }
}
