package com.overmild.mugs.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

import java.util.UUID;

@Value
public class Location {

    UUID id;

    @NotBlank
    String name;

    String description;
    Address address;
    String photoUrl;
}
