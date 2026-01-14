package com.overmild.mugs.entity;

import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;

@Data
@Entity(name = "mugs_location")
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private String name;

    @EqualsAndHashCode.Include
    private String description;

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MugEntity> mugs;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}



