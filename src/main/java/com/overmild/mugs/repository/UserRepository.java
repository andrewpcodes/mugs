package com.overmild.mugs.repository;

import com.overmild.mugs.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Fetches a user with all their mugs and the mugs' locations eagerly loaded.
     * Uses JOIN FETCH to avoid N+1 query problem.
     *
     * @param id the UUID of the user to fetch
     * @return an Optional containing the user with mugs and locations, or empty if not found
     */
    @Query("SELECT DISTINCT u FROM mugs_user u " +
           "LEFT JOIN FETCH u.mugs m " +
           "LEFT JOIN FETCH m.location " +
           "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithMugsAndLocations(@Param("id") UUID id);
}
