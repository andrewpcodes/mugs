package com.overmild.mugs.repository;

import com.overmild.mugs.entity.MugEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MugRepository extends JpaRepository<MugEntity, UUID> {

    /**
     * Fetches all mugs for a specific user with location eagerly loaded.
     * Uses JOIN FETCH to avoid N+1 query problem.
     *
     * @param userId the UUID of the user whose mugs to fetch
     * @return a list of mugs belonging to the user with locations loaded
     */
    @Query("SELECT m FROM mugs_mug m " +
           "LEFT JOIN FETCH m.location " +
           "WHERE m.user.id = :userId")
    List<MugEntity> findAllByUserId(@Param("userId") UUID userId);
}
