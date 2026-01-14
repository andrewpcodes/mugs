package com.overmild.mugs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@Entity(name = "mugs_mug")
public class MugEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "display_name", nullable = false)
    @EqualsAndHashCode.Include
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private LocationEntity location;
}
