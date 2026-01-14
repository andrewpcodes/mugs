package com.overmild.mugs.model;

import lombok.Value;

import java.util.UUID;

@Value
public class Location {

    UUID id;
    String name;
    String description;
}
