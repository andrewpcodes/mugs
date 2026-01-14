package com.overmild.mugs.service;

import com.overmild.mugs.entity.MugEntity;
import com.overmild.mugs.mapper.MugMapper;
import com.overmild.mugs.model.Mug;
import com.overmild.mugs.repository.MugRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing mug operations.
 * Provides business logic for CRUD operations on mugs.
 *
 * <p>This service acts as an intermediary between the controller layer
 * and the data access layer, handling mug entities and converting them
 * to/from domain models.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MugService {

    private final MugRepository repository;
    private final MugMapper mugMapper;

    /**
     * Retrieves all mugs from the database.
     *
     * @return a list of all mugs in the system
     */
    @Transactional
    public List<Mug> getAllMugs() {
        log.info("Fetching all mugs from the database");
        return repository.findAll()
                .stream()
                .map(mugMapper::toModel)
                .toList();
    }

    /**
     * Retrieves a mug by its unique identifier.
     *
     * @param id the UUID of the mug to retrieve
     * @return the mug with the specified ID, or null if not found
     */
    @Transactional
    public Mug getMugById(UUID id) {
        log.info("Fetching mug with ID: {}", id);
        return repository.findById(id)
                .map(mugMapper::toModel)
                .orElse(null);
    }

    /**
     * Retrieves all mugs for a specific user.
     * Uses an optimized query with JOIN FETCH to load locations in a single query.
     *
     * @param userId the UUID of the user whose mugs to retrieve
     * @return a list of all mugs belonging to the user
     */
    @Transactional
    public List<Mug> getMugsByUserId(UUID userId) {
        log.info("Fetching all mugs for user with ID: {}", userId);
        return repository.findAllByUserId(userId)
                .stream()
                .map(mugMapper::toModel)
                .toList();
    }

    /**
     * Creates a new mug in the database.
     *
     * @param mug the mug object to create
     * @return the created mug with all fields populated, including generated ID
     */
    public Mug createMug(Mug mug) {
        log.info("Creating new mug with id: {}", mug.getId());
        MugEntity entity = mugMapper.toEntity(mug);
        return mugMapper.toModel(repository.save(entity));
    }

    /**
     * Updates an existing mug in the database.
     *
     * <p>This method will update all fields of the mug. If the mug does not exist
     * or has a null ID, the update will fail and return null.</p>
     *
     * @param mug the mug object containing updated information
     * @return the updated mug, or null if the mug doesn't exist or ID is null
     */
    public Mug updateMug(Mug mug) {
        log.info("Updating mug with ID: {}", mug.getId());
        if (mug.getId() == null || repository.findById(mug.getId()).isEmpty()) {
            return null;
        }
        var entity = mugMapper.toEntity(mug);
        var updatedEntity = repository.save(entity);
        return mugMapper.toModel(updatedEntity);
    }

    /**
     * Deletes a mug from the database by its unique identifier.
     *
     * <p>This is a hard delete operation. If the mug does not exist,
     * this method will complete without throwing an exception.</p>
     *
     * @param id the UUID of the mug to delete
     */
    @Transactional
    public void deleteMug(UUID id) {
        log.info("Deleting mug with ID: {}", id);
        repository.deleteById(id);
    }
}
