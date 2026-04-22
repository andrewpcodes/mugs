package com.overmild.mugs.service;

import com.overmild.mugs.entity.AppUserEntity;
import com.overmild.mugs.exception.ConflictException;
import com.overmild.mugs.model.AppUserRegistration;
import com.overmild.mugs.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new application user with the USER role.
     *
     * @param registration the username and plaintext password for the new user
     * @throws ConflictException if the username is already taken
     */
    @Transactional
    public void register(AppUserRegistration registration) {
        if (appUserRepository.existsByUsername(registration.getUsername())) {
            throw new ConflictException("Username '" + registration.getUsername() + "' is already taken");
        }

        AppUserEntity entity = new AppUserEntity();
        entity.setUsername(registration.getUsername());
        entity.setPassword(passwordEncoder.encode(registration.getPassword()));
        entity.setRole("USER");

        appUserRepository.save(entity);
        log.info("Registered new user: {}", registration.getUsername());
    }
}
