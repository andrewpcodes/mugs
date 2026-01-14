package com.overmild.mugs.model;

import lombok.Value;

import java.util.UUID;

@Value
public class Mug {

    UUID id;
    String displayName;
    Location location;
    User user;
}
