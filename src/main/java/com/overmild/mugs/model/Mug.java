package com.overmild.mugs.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

import java.util.UUID;

@Value
public class Mug {

    UUID id;

    @NotBlank
    String displayName;

    Location location;
    User user;
}
