package com.overmild.mugs.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class AppUserRegistration {

    @NotBlank
    String username;

    @NotBlank
    String password;
}
