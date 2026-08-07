package photomarketplace.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import photomarketplace.model.entity.user.User;
import photomarketplace.repository.user.UserRepository;

@Service
public class MarketplaceUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    MarketplaceUserDetailsService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final User user = this.userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));

        return new MarketplaceUserDetails(user);
    }
}
