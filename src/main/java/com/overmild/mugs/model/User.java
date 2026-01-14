package com.overmild.mugs.model;

import java.util.UUID;
import lombok.Value;

@Value
public class User {

    UUID id;
    String firstName;
    String lastName;
    String email;
}
