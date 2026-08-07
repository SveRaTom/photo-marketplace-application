package photomarketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import photomarketplace.security.MarketplaceAuthenticationSuccessHandler;
import photomarketplace.security.MarketplaceUserDetailsService;

@Configuration
public class SecurityConfiguration {

    private final MarketplaceAuthenticationSuccessHandler authenticationSuccessHandler;
    private final MarketplaceUserDetailsService userDetailsService;

    SecurityConfiguration(final MarketplaceAuthenticationSuccessHandler authenticationSuccessHandler,
                          final MarketplaceUserDetailsService userDetailsService) {

        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .userDetailsService(this.userDetailsService)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/register", "/error", "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/offers",
                                "/offers/*",
                                "/offers/*/photos",
                                "/offers/*/reviews",
                                "/photos/*",
                                "/reviews/*",
                                "/photographers/*")
                        .permitAll()
                        .requestMatchers(
                                "/my-offers",
                                "/offers/create",
                                "/offers/edit/*",
                                "/offers/delete/*",
                                "/portfolio",
                                "/photos",
                                "/photos/create/*",
                                "/photos/edit/*",
                                "/photos/delete/*",
                                "/photos/*/cover",
                                "/bookings/*/approve",
                                "/bookings/*/reject",
                                "/dashboard")
                        .hasRole("PHOTOGRAPHER")
                        .requestMatchers(
                                "/offers/*/custom-offer",
                                "/bookings/create/*",
                                "/bookings/edit/*",
                                "/bookings/delete/*",
                                "/reviews/create/*",
                                "/reviews/edit/*",
                                "/reviews/delete/*")
                        .hasRole("CLIENT")
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(this.authenticationSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }
}
