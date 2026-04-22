package com.overmild.mugs.controller;

import com.overmild.mugs.model.AppUserRegistration;
import com.overmild.mugs.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /auth/register - register a new application user (role: USER)</li>
 * </ul>
 *
 * <p>Authentication uses HTTP Basic Auth. Credentials are validated against the
 * {@code app_user} table with BCrypt-encoded passwords.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new application user. The created account is assigned the USER role.
     * To obtain an ADMIN account, contact the system administrator.
     *
     * @param registration the requested username and plaintext password
     * @return 201 Created on success
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody AppUserRegistration registration) {
        authService.register(registration);
        return ResponseEntity.status(201).build();
    }
}
