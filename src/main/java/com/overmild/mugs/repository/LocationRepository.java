package com.overmild.mugs.repository;

import com.overmild.mugs.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {

    @Query("SELECT DISTINCT l FROM mugs_location l " +
            "LEFT JOIN FETCH l.mugs m " +
            "WHERE l.id = :id")
    Optional<LocationEntity> findByIdWithMugs(@Param("id") UUID id);
}
