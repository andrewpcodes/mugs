package com.overmild.mugs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Mugs API.
 *
 * <p>Authentication: HTTP Basic Auth backed by the {@code app_user} database table.
 * Passwords are stored as BCrypt hashes.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>GET requests on all endpoints are publicly accessible (no authentication required).</li>
 *   <li>{@code POST /auth/register} is publicly accessible (self-registration creates a USER account).</li>
 *   <li>All other mutating operations (POST, PUT, DELETE) require the {@code ADMIN} role.</li>
 * </ul>
 *
 * <p>Sessions are stateless; a valid {@code Authorization: Basic ...} header must be sent with
 * every request that requires authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
